// REST 服务端：协议插件、全部 v1 路由与生命周期。

package xin.ctkqiang.burpsuite.remote.transport

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import xin.ctkqiang.burpsuite.remote.adapter.BurpHistoryAdapter
import xin.ctkqiang.burpsuite.remote.adapter.BurpScopeAdapter
import xin.ctkqiang.burpsuite.remote.protocol.CommandResult
import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.protocol.RemoteClientMessage
import xin.ctkqiang.burpsuite.remote.protocol.RemoteError
import xin.ctkqiang.burpsuite.remote.protocol.RemoteErrorCode
import xin.ctkqiang.burpsuite.remote.protocol.RemoteMessageType
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolJson
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.protocol.RemoteResponse
import xin.ctkqiang.burpsuite.remote.security.DevicePairingService
import xin.ctkqiang.burpsuite.remote.security.PairedDeviceRegistry
import xin.ctkqiang.burpsuite.remote.security.PairingOutcome
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * 远程控制服务端。
 *
 * 监听端口只来自 [DEFAULT_REMOTE_PORT]，不写第二处字面量；历史类端点读的是 Burp 真实产生的代理历史，
 * 而拦截与 Repeater 在对应能力接入之前继续回「尚未实现」，编造出来的能力比明确的失败更糟。
 */
class RemoteHttpServer(
    private val devicePairingService: DevicePairingService,
    private val pairedDeviceRegistry: PairedDeviceRegistry,
    private val controlGate: RemoteControlGate,
    private val connectionRegistry: RemoteConnectionRegistry,
    private val eventStream: RemoteEventStream,
    private val historyAdapter: BurpHistoryAdapter,
    private val scopeAdapter: BurpScopeAdapter,
    private val logSink: (String) -> Unit,
    private val remotePort: Int = DEFAULT_REMOTE_PORT,
) {
    private val lifecycleLock = Any()

    // 事件通道与 REST 共用同一个日志出口：手机连不上时，两边的痕迹必须落在同一处才拼得出因果。
    private val webSocketServer = RemoteWebSocketServer(controlGate, connectionRegistry, eventStream, logSink)

    private var runningServer: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    /** 远程端点是否正在监听；界面据此显示已启动或未启动。 */
    val isRunning: Boolean
        get() = synchronized(lifecycleLock) { runningServer != null }

    /**
     * 开始监听。
     *
     * 端口被占用时记录一条中文日志并返回 false，而不是让异常逃逸：扩展加载失败会连带整个 Burp 报错，而这里只损失一个功能。
     */
    fun start(): Boolean {
        synchronized(lifecycleLock) {
            if (runningServer != null) {
                return true
            }
            if (!isPortAvailable()) {
                logSink("Burp Remote 远程端点启动失败，端口 $remotePort 已被占用；扩展继续以未启动状态运行。")
                return false
            }
            return try {
                runningServer = createAndStartServer()
                logSink("Burp Remote 远程端点已启动：$LISTEN_HOST_ADDRESS:$remotePort")
                true
            } catch (portUnavailable: IOException) {
                runningServer = null
                logSink(
                    "Burp Remote 远程端点启动失败，端口 $remotePort 在启动瞬间被抢占；扩展继续以未启动状态运行：" +
                        portUnavailable.message,
                )
                false
            }
        }
    }

    // 先自己试绑一次：直接让 Ktor 去撞已占用的端口，会留下一个失败的引擎协程，异常虽被捕获仍会飘到公共异常处理器上。
    private fun isPortAvailable(): Boolean =
        try {
            // 只探测能否绑定，不做 accept；reuseAddress 避免上一次监听把端口短暂钉在 TIME_WAIT。
            ServerSocket().use { probeSocket ->
                probeSocket.reuseAddress = true
                probeSocket.bind(InetSocketAddress(remotePort))
            }
            true
        } catch (expectedOccupiedPort: IOException) {
            false
        }

    /**
     * 停止监听并释放端口。
     *
     * 必须真正释放端口与线程，否则重新加载扩展时会因为它仍被自己占用而再次启动失败。
     */
    fun stop() {
        synchronized(lifecycleLock) {
            val server = runningServer ?: return
            runningServer = null
            server.stop(GRACE_PERIOD_MILLISECONDS, SHUTDOWN_TIMEOUT_MILLISECONDS)
            logSink("Burp Remote 远程端点已停止")
        }
    }

    /**
     * 把协议插件与全部路由装到 Ktor 应用上。
     *
     * 与 [start] 分开是为了让测试能在不起真实端口的前提下跑完整路由（rules.md §13）。
     */
    fun installTo(application: Application) {
        application.install(ContentNegotiation) {
            json(RemoteProtocolJson.instance)
        }
        webSocketServer.installTo(application)
        application.routing {
            get(STATUS_PATH) {
                call.respondAuthenticatedQuery(RemoteMessageType.Query) { buildRuntimeStatePayload() }
            }
            post(PAIR_PATH) {
                call.respondPairing()
            }
            get(CAPABILITIES_PATH) {
                call.respondAuthenticatedQuery(RemoteMessageType.Query) { buildCapabilitiesPayload() }
            }
            get(SNAPSHOT_PATH) {
                call.respondAuthenticatedQuery(RemoteMessageType.Snapshot) { buildRuntimeStatePayload() }
            }

            // 历史类端点已接上 Burp 适配层：列表只回元数据，正文走按标识取回的那条。
            get(HISTORY_PATH) {
                call.respondAuthenticatedQuery(RemoteMessageType.Query) { historyAdapter.buildHistoryListPayload() }
            }
            get(HISTORY_ITEM_PATH) {
                call.respondAuthenticatedQuery(RemoteMessageType.Query) {
                    call.parameters[HISTORY_IDENTIFIER_ROUTE_PARAMETER]
                        ?.let { identifierText ->
                            historyAdapter.buildHistoryMessagePayload(HistoryIdentifier(identifierText))
                        }
                }
            }

            // 加入作用域：手机只发历史标识，主机串由插件读 Burp 当下的事实拼出，过期记录也写不错地址。
            // 写入落在执行动作里：幂等重放走的是首次记录的结果，不会再写一次 Burp 作用域。
            post(SCOPE_PATH) {
                val historyIdentifierText =
                    call.parameters[HISTORY_IDENTIFIER_ROUTE_PARAMETER]
                        ?.takeIf { identifierText -> identifierText.isNotBlank() }
                call.respondControlCommand(COMMAND_TYPE_SCOPE_INCLUDE) { operationIdentifier ->
                    // 没有对应记录时主机串为 null：不新增错误码，复用既有的运行时失败码（错误码集合是跨端协议）。
                    historyIdentifierText
                        ?.let { identifierText ->
                            scopeAdapter.includeHistoryHostInScope(HistoryIdentifier(identifierText))
                        }
                        ?.let { CommandResult.Succeeded(operationIdentifier) }
                        ?: failedResult(RemoteErrorCode.BurpRuntimeFailure, isRetryable = false)
                }
            }

            // 以下端点仍然依赖尚未实现的 Burp 能力（拦截队列、Repeater），因此只回「尚未实现」。
            get(INTERCEPTS_PATH) { call.respondAuthenticatedNotImplemented(RemoteMessageType.Query) }
            get(INTERCEPT_ITEM_PATH) { call.respondAuthenticatedNotImplemented(RemoteMessageType.Query) }
            post(INTERCEPT_MODIFY_PATH) { call.respondControlCommand(COMMAND_TYPE_INTERCEPT_MODIFY) }
            post(INTERCEPT_FORWARD_PATH) { call.respondControlCommand(COMMAND_TYPE_INTERCEPT_FORWARD) }
            post(INTERCEPT_DROP_PATH) { call.respondControlCommand(COMMAND_TYPE_INTERCEPT_DROP) }
            post(REPEATER_PATH) { call.respondControlCommand(COMMAND_TYPE_REPEATER_CREATE) }
            post(REPEATER_EXECUTE_PATH) { call.respondControlCommand(COMMAND_TYPE_REPEATER_EXECUTE) }
        }
    }

    private fun createAndStartServer(): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> =
        embeddedServer(
            factory = CIO,
            port = remotePort,
            host = LISTEN_HOST_ADDRESS,
            module = { installTo(this) },
        ).start(wait = false)

    // 查询类端点同样要鉴权：局域网不等于可信网络，能读历史就等于能读凭证。
    // 载荷延后到鉴权之后才计算：未配对的请求连一次 Burp 历史都不该读；载荷为 null 表示 Burp 已经拿不出这条记录了。
    private suspend fun ApplicationCall.respondAuthenticatedQuery(
        messageType: RemoteMessageType,
        payloadSupplier: () -> JsonElement?,
    ) {
        if (controlGate.resolveAuthenticatedDevice(readDeviceIdentifier()) == null) {
            respondRejected(messageType, RejectionReason.DeviceNotPaired)
            return
        }
        val payload = payloadSupplier()
        if (payload == null) {
            // 不新增错误码：错误码集合是跨端协议，新增一个会让尚未同步的客户端在解析应答时直接失败。
            respondProtocolResult(messageType, failedResult(RemoteErrorCode.BurpRuntimeFailure, isRetryable = false))
            return
        }
        respond(
            RemoteResponse(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                messageType = messageType,
                payload = payload,
            ),
        )
    }

    private suspend fun ApplicationCall.respondAuthenticatedNotImplemented(messageType: RemoteMessageType) {
        if (controlGate.resolveAuthenticatedDevice(readDeviceIdentifier()) == null) {
            respondRejected(messageType, RejectionReason.DeviceNotPaired)
            return
        }
        respondProtocolResult(messageType, failedResult(RemoteErrorCode.NotImplemented, isRetryable = false))
    }

    // 控制类端点共用这一条路径：命令类型由路由给出，执行动作由调用方给出。
    // 默认动作回「尚未实现」：拦截队列与 Repeater 还没接入，编造出来的能力比明确的失败更糟。
    // 幂等重放由闸门在进入执行动作之前拦下，因此执行动作里的副作用天然只发生一次。
    private suspend fun ApplicationCall.respondControlCommand(
        commandType: String,
        executeCommand: (OperationIdentifier) -> CommandResult = { _ ->
            failedResult(RemoteErrorCode.NotImplemented, isRetryable = false)
        },
    ) {
        val commandResult =
            controlGate.handleControlCommand(
                submittedDeviceIdentifier = readDeviceIdentifier(),
                submittedOperationIdentifier = readOperationIdentifier(),
                commandType = commandType,
                executeCommand = executeCommand,
            )
        respondProtocolResult(RemoteMessageType.Command, commandResult)
    }

    private suspend fun ApplicationCall.respondPairing() {
        if (rejectOversizedRequest()) {
            return
        }

        val clientMessage = receiveClientMessage()
        val challengeIdentifier = clientMessage?.challengeIdentifier
        val pairingCode = clientMessage?.pairingCode
        if (clientMessage == null ||
            clientMessage.messageType != RemoteMessageType.Pair ||
            challengeIdentifier == null ||
            pairingCode == null
        ) {
            respondProtocolResult(
                RemoteMessageType.Pair,
                failedResult(RemoteErrorCode.SerializationFailure, isRetryable = true),
            )
            return
        }

        when (val pairingOutcome = devicePairingService.redeemPairingCode(challengeIdentifier, pairingCode)) {
            is PairingOutcome.Succeeded -> respond(buildPairingSucceededResponse(pairingOutcome))
            is PairingOutcome.Rejected -> respondRejected(RemoteMessageType.Pair, pairingOutcome.reason)
        }
    }

    private fun buildPairingSucceededResponse(pairingOutcome: PairingOutcome.Succeeded): RemoteResponse =
        RemoteResponse(
            protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
            messageType = RemoteMessageType.Pair,
            payload =
                buildJsonObject {
                    // 只回身份本身：配对凭据属于机密，绝不随应答回传（rules.md §12）。
                    put(DEVICE_IDENTIFIER_FIELD, pairingOutcome.deviceIdentifier.value)
                },
        )

    private fun buildRuntimeStatePayload(): JsonElement =
        buildJsonObject {
            put(PROTOCOL_VERSION_FIELD, RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION)
            put(REMOTE_PORT_FIELD, remotePort)
            put(CONNECTED_DEVICE_COUNT_FIELD, connectionRegistry.connectedDeviceCount())
            put(PAIRED_DEVICE_COUNT_FIELD, pairedDeviceRegistry.snapshot().size)
            put(LATEST_EVENT_SEQUENCE_NUMBER_FIELD, eventStream.latestSequenceNumber())
        }

    // 只列已经实现的能力：把未实现的也报上去，客户端会照着一个不存在的功能去设计交互。
    private fun buildCapabilitiesPayload(): JsonElement =
        buildJsonObject {
            putJsonArray(CAPABILITIES_FIELD) {
                add(CAPABILITY_STATUS)
                add(CAPABILITY_PAIRING)
                add(CAPABILITY_EVENT_STREAM)
                add(CAPABILITY_SNAPSHOT)
                add(CAPABILITY_HISTORY)
                add(CAPABILITY_SCOPE)
            }
        }

    private suspend fun ApplicationCall.respondProtocolResult(
        messageType: RemoteMessageType,
        result: CommandResult,
    ) {
        respond(
            RemoteResponse(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                messageType = messageType,
                result = result,
            ),
        )
    }

    private suspend fun ApplicationCall.respondRejected(
        messageType: RemoteMessageType,
        rejectionReason: RejectionReason,
    ) {
        respondProtocolResult(messageType, CommandResult.Rejected(rejectionReason))
    }

    // 只按声明的长度拦截：分块请求没有 Content-Length，这一层拦不住，WebSocket 方向则由帧上限兜底。
    private suspend fun ApplicationCall.rejectOversizedRequest(): Boolean {
        val declaredContentLength = request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (declaredContentLength == null || declaredContentLength <= MAXIMUM_REQUEST_BODY_BYTES) {
            return false
        }
        respond(
            HttpStatusCode.PayloadTooLarge,
            RemoteResponse(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                messageType = RemoteMessageType.Command,
                result = failedResult(RemoteErrorCode.RequestPayloadTooLarge, isRetryable = false),
            ),
        )
        return true
    }

    private suspend fun ApplicationCall.receiveClientMessage(): RemoteClientMessage? =
        try {
            RemoteProtocolJson.instance.decodeFromString(RemoteClientMessage.serializer(), receiveText())
        } catch (expectedMalformedMessage: SerializationException) {
            null
        }

    private fun ApplicationCall.readDeviceIdentifier(): DeviceIdentifier? =
        request.headers[DEVICE_IDENTIFIER_HEADER]?.takeIf { it.isNotBlank() }?.let { DeviceIdentifier(it) }

    private fun ApplicationCall.readOperationIdentifier(): OperationIdentifier? =
        request.headers[OPERATION_IDENTIFIER_HEADER]?.takeIf { it.isNotBlank() }?.let { OperationIdentifier(it) }

    private fun failedResult(
        errorCode: RemoteErrorCode,
        isRetryable: Boolean,
    ): CommandResult = CommandResult.Failed(RemoteError(errorCode, isRetryable))

    private companion object {
        // 绑全部网卡：配对票据里写的是局域网地址，只绑回环手机就连不上。
        private const val LISTEN_HOST_ADDRESS = "0.0.0.0"

        private const val DEVICE_IDENTIFIER_HEADER = "X-Burp-Remote-Device-Identifier"

        private const val OPERATION_IDENTIFIER_HEADER = "X-Burp-Remote-Operation-Identifier"

        // 请求体上限原文未定义，此处选定值：64 KiB。
        // 依据：本协议只传命令与查询参数，正文一律按标识另行取；64 KiB 对参数足够宽松，又能挡住整段报文上传。
        private const val MAXIMUM_REQUEST_BODY_BYTES = 65_536L

        private const val GRACE_PERIOD_MILLISECONDS = 500L

        private const val SHUTDOWN_TIMEOUT_MILLISECONDS = 1_000L

        private const val STATUS_PATH = "/v1/status"

        private const val PAIR_PATH = "/v1/pair"

        private const val CAPABILITIES_PATH = "/v1/capabilities"

        private const val SNAPSHOT_PATH = "/v1/snapshot"

        private const val HISTORY_PATH = "/v1/history"

        private const val HISTORY_ITEM_PATH = "/v1/history/{historyIdentifier}"

        private const val HISTORY_IDENTIFIER_ROUTE_PARAMETER = "historyIdentifier"

        private const val SCOPE_PATH = "/v1/scope/{historyIdentifier}"

        private const val INTERCEPTS_PATH = "/v1/intercepts"

        private const val INTERCEPT_ITEM_PATH = "/v1/intercepts/{interceptIdentifier}"

        private const val INTERCEPT_MODIFY_PATH = "/v1/intercepts/{interceptIdentifier}/modify"

        private const val INTERCEPT_FORWARD_PATH = "/v1/intercepts/{interceptIdentifier}/forward"

        private const val INTERCEPT_DROP_PATH = "/v1/intercepts/{interceptIdentifier}/drop"

        private const val REPEATER_PATH = "/v1/repeater"

        private const val REPEATER_EXECUTE_PATH = "/v1/repeater/{repeaterRequestIdentifier}/execute"

        private const val COMMAND_TYPE_INTERCEPT_MODIFY = "intercept.modify"

        private const val COMMAND_TYPE_INTERCEPT_FORWARD = "intercept.forward"

        private const val COMMAND_TYPE_INTERCEPT_DROP = "intercept.drop"

        private const val COMMAND_TYPE_REPEATER_CREATE = "repeater.create"

        private const val COMMAND_TYPE_REPEATER_EXECUTE = "repeater.execute"

        private const val COMMAND_TYPE_SCOPE_INCLUDE = "scope.include"

        private const val DEVICE_IDENTIFIER_FIELD = "deviceIdentifier"

        private const val PROTOCOL_VERSION_FIELD = "protocolVersion"

        private const val REMOTE_PORT_FIELD = "remotePort"

        private const val CONNECTED_DEVICE_COUNT_FIELD = "connectedDeviceCount"

        private const val PAIRED_DEVICE_COUNT_FIELD = "pairedDeviceCount"

        private const val LATEST_EVENT_SEQUENCE_NUMBER_FIELD = "latestEventSequenceNumber"

        private const val CAPABILITIES_FIELD = "capabilities"

        private const val CAPABILITY_STATUS = "status"

        private const val CAPABILITY_PAIRING = "pairing"

        private const val CAPABILITY_EVENT_STREAM = "event-stream"

        private const val CAPABILITY_SNAPSHOT = "snapshot"

        private const val CAPABILITY_HISTORY = "history"

        private const val CAPABILITY_SCOPE = "scope"
    }
}
