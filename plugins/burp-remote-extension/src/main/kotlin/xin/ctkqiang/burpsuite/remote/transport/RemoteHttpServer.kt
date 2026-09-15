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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import burp.api.montoya.http.message.requests.HttpRequest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import burp.api.montoya.http.Http as MontoyaHttp
import xin.ctkqiang.burpsuite.remote.adapter.BurpHistoryAdapter
import xin.ctkqiang.burpsuite.remote.adapter.BurpInterceptQueue
import xin.ctkqiang.burpsuite.remote.adapter.BurpRepeaterStore
import xin.ctkqiang.burpsuite.remote.adapter.BurpScopeAdapter
import xin.ctkqiang.burpsuite.remote.adapter.InterceptDecision
import xin.ctkqiang.burpsuite.remote.adapter.MontoyaInterceptedRequest
import xin.ctkqiang.burpsuite.remote.protocol.CommandResult
import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.protocol.RepeaterRequestIdentifier
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
import xin.ctkqiang.burpsuite.remote.transport.InMemoryRemoteEventStream
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
    private val eventStream: InMemoryRemoteEventStream,
    private val historyAdapter: BurpHistoryAdapter,
    private val scopeAdapter: BurpScopeAdapter,
    private val logSink: (String) -> Unit,
    private val remotePort: Int = DEFAULT_REMOTE_PORT,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
    private val interceptQueue: BurpInterceptQueue? = null,
    private val sendToRepeater: ((HttpRequest, String?) -> Unit)? = null,
    /** Remote Repeater 内存存储；为 null 时 repeater 端点回 NotImplemented。 */
    private val repeaterStore: BurpRepeaterStore? = null,
    /** Montoya HTTP 服务，用于远程执行 repeater 请求（sendRequest）。 */
    private val montoyaHttp: MontoyaHttp? = null,
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
                // 序号由 InMemoryRemoteEventStream 统一分配，这里不需要再对齐。
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

            // 拦截类端点：队列是真实的挂起点，REST 端点直接读写它。
            get(INTERCEPTS_PATH) {
                val queue = interceptQueue
                if (queue == null) {
                    call.respondAuthenticatedNotImplemented(RemoteMessageType.Query)
                    return@get
                }
                call.respondAuthenticatedQuery(RemoteMessageType.Query) {
                    buildJsonObject {
                        putJsonArray(INTERCEPT_ITEMS_FIELD) {
                            queue.snapshot().forEach { wrapped -> add(wrapped.buildMetadataPayload()) }
                        }
                    }
                }
            }
            get(INTERCEPT_ITEM_PATH) {
                val queue = interceptQueue
                if (queue == null) {
                    call.respondAuthenticatedNotImplemented(RemoteMessageType.Query)
                    return@get
                }
                call.respondAuthenticatedQuery(RemoteMessageType.Query) {
                    call.parameters[INTERCEPT_IDENTIFIER_ROUTE_PARAMETER]
                        ?.let { identifierText -> InterceptIdentifier(identifierText) }
                        ?.let { queue.find(it) }
                        ?.let { wrapped -> wrapped.buildMessagePayload() }
                }
            }
            post(INTERCEPT_FORWARD_PATH) {
                call.handleInterceptForward()
            }
            post(INTERCEPT_DROP_PATH) {
                call.handleInterceptDrop()
            }
            post(INTERCEPT_MODIFY_PATH) {
                call.handleInterceptModify()
            }

            // Repeater 列表 + 单条详情
            get(REPEATERS_PATH) {
                val store = repeaterStore
                if (store == null) {
                    call.respondAuthenticatedNotImplemented(RemoteMessageType.Query)
                    return@get
                }
                call.respondAuthenticatedQuery(RemoteMessageType.Query) {
                    buildJsonObject {
                        putJsonArray(REPEATER_ITEMS_FIELD) {
                            store.snapshot().forEach { add(buildRepeaterItemPayload(it)) }
                        }
                    }
                }
            }
            get(REPEATER_ITEM_PATH) {
                val store = repeaterStore
                if (store == null) {
                    call.respondAuthenticatedNotImplemented(RemoteMessageType.Query)
                    return@get
                }
                val identifier = call.parameters[REPEATER_IDENTIFIER_ROUTE_PARAMETER]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { xin.ctkqiang.burpsuite.remote.protocol.RepeaterRequestIdentifier(it) }
                val stored = identifier?.let { store.find(it) }
                call.respondAuthenticatedQuery(RemoteMessageType.Query) {
                    stored?.let { buildRepeaterItemPayload(it) }
                }
            }

            // Repeater create：手机推 requestText → 插件存 store → 同时 sendToRepeater 到 Burp PC tab → 发 created 事件
            post(REPEATER_PATH) {
                call.handleRepeaterCreate()
            }

            // Repeater execute：真执行 — 用 montoyaApi.http().sendRequest() 发请求，发 started/completed 事件，存结果回 store
            post(REPEATER_EXECUTE_PATH) {
                call.handleRepeaterExecute()
            }
        }
    }

    // --- Repeater 命令实现 ---

    private suspend fun ApplicationCall.handleRepeaterCreate() {
        val store = repeaterStore
        if (store == null) {
            respondControlCommand(COMMAND_TYPE_REPEATER_CREATE)
            return
        }
        val body = receiveText()
        if (body.isBlank()) {
            respondControlCommand(COMMAND_TYPE_REPEATER_CREATE) { _ ->
                failedResult(RemoteErrorCode.SerializationFailure, isRetryable = true)
            }
            return
        }
        val payload =
            try {
                RemoteProtocolJson.instance.parseToJsonElement(body).jsonObject
            } catch (_: Exception) {
                null
            }
        val requestText = (payload?.get(REQUEST_TEXT_FIELD) as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
        val tabName = (payload?.get(TAB_NAME_FIELD) as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
        if (requestText == null) {
            respondControlCommand(COMMAND_TYPE_REPEATER_CREATE) { _ ->
                failedResult(RemoteErrorCode.SerializationFailure, isRetryable = true)
            }
            return
        }
        // 先让 Montoya 解析一遍，防止存进去的 requestText 根本不是合法 HTTP 请求
        try {
            val request = HttpRequest.httpRequest(requestText)
            val stored = store.create(requestText, tabName, java.time.Instant.now(clock))
            // 有 sendToRepeater 回调就顺便推到 Burp PC 的 Repeater tab
            sendToRepeater?.invoke(request, tabName)
            // 发 repeater.created 事件，让手机端 projection 知道有新条目了
            emitRepeaterCreated(stored, java.time.Instant.now(clock))
            respondControlCommand(COMMAND_TYPE_REPEATER_CREATE) { op ->
                CommandResult.Succeeded(op)
            }
        } catch (_: Exception) {
            respondControlCommand(COMMAND_TYPE_REPEATER_CREATE) { _ ->
                failedResult(RemoteErrorCode.BurpRuntimeFailure, isRetryable = false)
            }
        }
    }

    private suspend fun ApplicationCall.handleRepeaterExecute() {
        val store = repeaterStore
        val http = montoyaHttp
        if (store == null || http == null) {
            respondControlCommand(COMMAND_TYPE_REPEATER_EXECUTE)
            return
        }
        val identifier = parameters[REPEATER_IDENTIFIER_ROUTE_PARAMETER]
            ?.takeIf { it.isNotBlank() }
            ?.let { xin.ctkqiang.burpsuite.remote.protocol.RepeaterRequestIdentifier(it) }
        val stored = identifier?.let { store.find(it) }
        if (stored == null) {
            respondControlCommand(COMMAND_TYPE_REPEATER_EXECUTE) { _ ->
                failedResult(RemoteErrorCode.EntityNotFound, isRetryable = false)
            }
            return
        }

        // 发 started 事件 — 让手机知道正在执行
        emitRepeaterStarted(identifier, java.time.Instant.now(clock))

        val (response, durationMs) = try {
            @Suppress("DEPRECATION")
            val request = HttpRequest.httpRequest(stored.requestText)
            val start = System.currentTimeMillis()
            val httpResponse = http.sendRequest(request)
            val end = System.currentTimeMillis()
            httpResponse to (end - start)
        } catch (t: Throwable) {
            // 哪怕发请求抛异常（DNS 失败、TLS 握手失败等），也要发 completed 事件，带上失败事实
            val now = java.time.Instant.now(clock)
            store.updateWithResult(identifier, null, -1L, now)
            emitRepeaterCompleted(identifier, now, failed = true, statusCode = null, durationMs = null)
            respondControlCommand(COMMAND_TYPE_REPEATER_EXECUTE) { _ ->
                failedResult(RemoteErrorCode.BurpRuntimeFailure, isRetryable = false)
            }
            return
        }

        val now = java.time.Instant.now(clock)
        store.updateWithResult(identifier, response, durationMs, now)
        @Suppress("DEPRECATION")
        val statusCode = response?.statusCode()?.toInt()
        emitRepeaterCompleted(
            identifier,
            now,
            failed = false,
            statusCode = statusCode,
            durationMs = durationMs,
        )
        respondControlCommand(COMMAND_TYPE_REPEATER_EXECUTE) { op -> CommandResult.Succeeded(op) }
    }

    private fun buildRepeaterItemPayload(stored: xin.ctkqiang.burpsuite.remote.adapter.StoredRepeaterRequest): kotlinx.serialization.json.JsonObject =
        buildJsonObject {
            put(REPEATER_IDENTIFIER_FIELD, stored.identifier.value)
            put(CREATED_AT_FIELD, stored.createdAtEpochMilliseconds)
            stored.tabName?.let { put(TAB_NAME_FIELD, it) }
            put(REQUEST_TEXT_FIELD, stored.requestText)
            stored.lastExecutedAtEpochMilliseconds?.let { put(LAST_EXECUTED_AT_FIELD, it) }
            stored.lastStatusCode?.let { put(LAST_STATUS_CODE_FIELD, it) }
            stored.lastResponseHeaders?.let { put(LAST_RESPONSE_HEADERS_FIELD, it) }
            stored.lastResponseBody?.let { put(LAST_RESPONSE_BODY_FIELD, it) }
            stored.lastDurationMilliseconds?.let { put(LAST_DURATION_FIELD, it) }
        }

    private fun emitRepeaterCreated(
        stored: xin.ctkqiang.burpsuite.remote.adapter.StoredRepeaterRequest,
        occurredAt: java.time.Instant,
    ) {
        // sequenceNumber 与 eventIdentifier 由 InMemoryRemoteEventStream.appendEvent 统一分配，
        // 这里传占位值即可——避免与 History/Intercept 发布者各自维护计数器导致撞号。
        eventStream.appendEvent(
            xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                eventIdentifier = xin.ctkqiang.burpsuite.remote.protocol.EventIdentifier(""),
                sequenceNumber = 0L,
                occurredAt = occurredAt,
                eventType = REPEATER_CREATED_EVENT_TYPE,
                aggregateType = xin.ctkqiang.burpsuite.remote.protocol.AggregateType.RepeaterRequest,
                aggregateIdentifier = xin.ctkqiang.burpsuite.remote.protocol.AggregateIdentifier(stored.identifier.value),
                payload = buildJsonObject {
                    put(REPEATER_IDENTIFIER_FIELD, stored.identifier.value)
                    put(REQUEST_TEXT_FIELD, stored.requestText)
                    stored.tabName?.let { put(TAB_NAME_FIELD, it) }
                    put(CREATED_AT_FIELD, stored.createdAtEpochMilliseconds)
                },
            ),
        )
    }

    private fun emitRepeaterStarted(
        identifier: xin.ctkqiang.burpsuite.remote.protocol.RepeaterRequestIdentifier,
        occurredAt: java.time.Instant,
    ) {
        eventStream.appendEvent(
            xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                eventIdentifier = xin.ctkqiang.burpsuite.remote.protocol.EventIdentifier(""),
                sequenceNumber = 0L,
                occurredAt = occurredAt,
                eventType = REPEATER_EXECUTION_STARTED_EVENT_TYPE,
                aggregateType = xin.ctkqiang.burpsuite.remote.protocol.AggregateType.RepeaterRequest,
                aggregateIdentifier = xin.ctkqiang.burpsuite.remote.protocol.AggregateIdentifier(identifier.value),
                payload = buildJsonObject {
                    put(REPEATER_IDENTIFIER_FIELD, identifier.value)
                },
            ),
        )
    }

    private fun emitRepeaterCompleted(
        identifier: xin.ctkqiang.burpsuite.remote.protocol.RepeaterRequestIdentifier,
        occurredAt: java.time.Instant,
        failed: Boolean,
        statusCode: Int?,
        durationMs: Long?,
    ) {
        eventStream.appendEvent(
            xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                eventIdentifier = xin.ctkqiang.burpsuite.remote.protocol.EventIdentifier(""),
                sequenceNumber = 0L,
                occurredAt = occurredAt,
                eventType = REPEATER_EXECUTION_COMPLETED_EVENT_TYPE,
                aggregateType = xin.ctkqiang.burpsuite.remote.protocol.AggregateType.RepeaterRequest,
                aggregateIdentifier = xin.ctkqiang.burpsuite.remote.protocol.AggregateIdentifier(identifier.value),
                payload = buildJsonObject {
                    put(REPEATER_IDENTIFIER_FIELD, identifier.value)
                    put(EXECUTION_FAILED_FIELD, failed)
                    statusCode?.let { put(LAST_STATUS_CODE_FIELD, it) }
                    durationMs?.let { put(LAST_DURATION_FIELD, it) }
                    put(LAST_EXECUTED_AT_FIELD, occurredAt.toEpochMilli())
                },
            ),
        )
    }

    // --- Intercept 命令实现 ---

    private suspend fun ApplicationCall.handleInterceptForward() {
        val queue = interceptQueue
        if (queue == null) {
            respondControlCommand(COMMAND_TYPE_INTERCEPT_FORWARD)
            return
        }
        val identifier = readInterceptIdentifier()
        if (identifier == null) {
            respondControlCommand(COMMAND_TYPE_INTERCEPT_FORWARD) { _ ->
                failedResult(RemoteErrorCode.SerializationFailure, isRetryable = true)
            }
            return
        }
        respondControlCommand(COMMAND_TYPE_INTERCEPT_FORWARD) { op ->
            if (queue.submitDecision(identifier, InterceptDecision.Forward())) {
                CommandResult.Succeeded(op)
            } else {
                CommandResult.Succeeded(op)
            }
        }
    }

    private suspend fun ApplicationCall.handleInterceptDrop() {
        val queue = interceptQueue
        if (queue == null) {
            respondControlCommand(COMMAND_TYPE_INTERCEPT_DROP)
            return
        }
        val identifier = readInterceptIdentifier()
        if (identifier == null) {
            respondControlCommand(COMMAND_TYPE_INTERCEPT_DROP) { _ ->
                failedResult(RemoteErrorCode.SerializationFailure, isRetryable = true)
            }
            return
        }
        respondControlCommand(COMMAND_TYPE_INTERCEPT_DROP) { op ->
            if (queue.submitDecision(identifier, InterceptDecision.Drop)) {
                CommandResult.Succeeded(op)
            } else {
                CommandResult.Succeeded(op)
            }
        }
    }

    private suspend fun ApplicationCall.handleInterceptModify() {
        val queue = interceptQueue
        if (queue == null) {
            respondControlCommand(COMMAND_TYPE_INTERCEPT_MODIFY)
            return
        }
        val identifier = readInterceptIdentifier()
        if (identifier == null) {
            respondControlCommand(COMMAND_TYPE_INTERCEPT_MODIFY) { _ ->
                failedResult(RemoteErrorCode.SerializationFailure, isRetryable = true)
            }
            return
        }
        val body = receiveText()
        val requestText =
            try {
                RemoteProtocolJson.instance.parseToJsonElement(body)
                    .jsonObject
                    .get(REQUEST_TEXT_FIELD)
                    ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
            } catch (_: Exception) {
                null
            }
        if (requestText == null) {
            respondControlCommand(COMMAND_TYPE_INTERCEPT_MODIFY) { _ ->
                failedResult(RemoteErrorCode.SerializationFailure, isRetryable = true)
            }
            return
        }
        respondControlCommand(COMMAND_TYPE_INTERCEPT_MODIFY) { op ->
            try {
                val modified = HttpRequest.httpRequest(requestText)
                queue.submitDecision(identifier, InterceptDecision.Forward(modified))
                CommandResult.Succeeded(op)
            } catch (_: Exception) {
                failedResult(RemoteErrorCode.BurpRuntimeFailure, isRetryable = false)
            }
        }
    }

    private fun ApplicationCall.readInterceptIdentifier(): InterceptIdentifier? =
        parameters[INTERCEPT_IDENTIFIER_ROUTE_PARAMETER]
            ?.takeIf { it.isNotBlank() }
            ?.let { InterceptIdentifier(it) }

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
    // interceptQueue 和 sendToRepeater 为 null 时对应的能力不列出——比如测试场景。
    private fun buildCapabilitiesPayload(): JsonElement =
        buildJsonObject {
            putJsonArray(CAPABILITIES_FIELD) {
                add(CAPABILITY_STATUS)
                add(CAPABILITY_PAIRING)
                add(CAPABILITY_EVENT_STREAM)
                add(CAPABILITY_SNAPSHOT)
                add(CAPABILITY_HISTORY)
                add(CAPABILITY_SCOPE)
                if (interceptQueue != null) add(CAPABILITY_INTERCEPT)
                if (sendToRepeater != null) add(CAPABILITY_REPEATER)
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

        private const val REPEATERS_PATH = "/v1/repeaters"

        private const val REPEATER_ITEM_PATH = "/v1/repeaters/{repeaterRequestIdentifier}"

        private const val REPEATER_EXECUTE_PATH = "/v1/repeater/{repeaterRequestIdentifier}/execute"

        private const val REPEATER_IDENTIFIER_ROUTE_PARAMETER = "repeaterRequestIdentifier"

        private const val REPEATER_ITEMS_FIELD = "repeaterItems"

        private const val REPEATER_IDENTIFIER_FIELD = "repeaterRequestIdentifier"

        private const val REQUEST_TEXT_FIELD = "requestText"

        private const val TAB_NAME_FIELD = "tabName"

        private const val CREATED_AT_FIELD = "createdAtEpochMilliseconds"

        private const val LAST_EXECUTED_AT_FIELD = "lastExecutedAtEpochMilliseconds"

        private const val LAST_STATUS_CODE_FIELD = "lastStatusCode"

        private const val LAST_RESPONSE_HEADERS_FIELD = "lastResponseHeaders"

        private const val LAST_RESPONSE_BODY_FIELD = "lastResponseBody"

        private const val LAST_DURATION_FIELD = "lastDurationMilliseconds"

        private const val EXECUTION_FAILED_FIELD = "failed"

        private const val REPEATER_CREATED_EVENT_TYPE = "repeater.created"

        private const val REPEATER_EXECUTION_STARTED_EVENT_TYPE = "repeater.execution.started"

        private const val REPEATER_EXECUTION_COMPLETED_EVENT_TYPE = "repeater.execution.completed"

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

        private const val CAPABILITY_INTERCEPT = "intercept"

        private const val CAPABILITY_REPEATER = "repeater"

        private const val INTERCEPT_IDENTIFIER_ROUTE_PARAMETER = "interceptIdentifier"

        private const val INTERCEPT_ITEMS_FIELD = "interceptItems"
    }
}
