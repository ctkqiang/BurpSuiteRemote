// 事件通道：WebSocket 握手、认证、断点续传。

package xin.ctkqiang.burpsuite.remote.transport

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RemoteClientMessage
import xin.ctkqiang.burpsuite.remote.protocol.RemoteConnectionCode
import xin.ctkqiang.burpsuite.remote.protocol.RemoteConnectionSignal
import xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope
import xin.ctkqiang.burpsuite.remote.protocol.RemoteMessageType
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolJson
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion

/**
 * 事件通道服务端（plan §50）。
 *
 * 握手顺序固定为 CONNECT → AUTHENTICATE → RESUME → events。
 * 认证之前一条事件都不会推送：未认证的连接既不订阅事件流，也没有任何可回放的数据来源。
 *
 * 每一次被拒的握手都留一行日志：手机连不上时这条通道此前在两端都不出声，排查只能靠猜。
 * 日志只写消息类别、协议版本、续传序号与设备身份，不写收到的报文原文——原文可能带着配对码（rules.md §12）。
 *
 * @param logSink 日志出口；由装配层指向 Burp 的输出面板。
 */
class RemoteWebSocketServer(
    private val controlGate: RemoteControlGate,
    private val connectionRegistry: RemoteConnectionRegistry,
    private val eventStream: RemoteEventStream,
    private val logSink: (String) -> Unit,
) {
    /**
     * 把事件通道装到 Ktor 应用上。
     *
     * 插件与路由一起装：只装路由而指望调用方记得先装插件，会在复用与测试时得到一个「路由在、却立刻失败」的假象。
     */
    fun installTo(application: Application) {
        application.install(WebSockets) {
            maxFrameSize = MAXIMUM_FRAME_SIZE_BYTES
            pingPeriodMillis = PING_PERIOD_MILLISECONDS
            timeoutMillis = SESSION_TIMEOUT_MILLISECONDS
            // 服务端不掩码：掩码只是客户端到服务端方向的义务，服务端掩码会被客户端判为协议错误。
            masking = false
        }
        application.routing {
            webSocket(EVENTS_PATH) {
                handleEventSession()
            }
        }
    }

    // 名额先占、握手后做：未认证的洪泛同样要能被连接数上限挡住。
    private suspend fun DefaultWebSocketServerSession.handleEventSession() {
        if (!connectionRegistry.openConnection()) {
            logSink("事件通道连接数已达上限，拒绝了这次连接")
            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, CONNECTION_LIMIT_REASON))
            return
        }

        var authenticatedDevice: DeviceIdentifier? = null
        try {
            authenticatedDevice = performHandshake() ?: return
            connectionRegistry.associateDevice(authenticatedDevice)
            awaitResumeAndStreamEvents()
        } finally {
            authenticatedDevice?.let(connectionRegistry::disassociateDevice)
            connectionRegistry.closeConnection()
            // 收尾也留一行：连上来又立刻断开这件事，只有这里说得清是哪台设备。
            logSink(
                "事件通道会话结束" +
                    (authenticatedDevice?.let { device -> "，设备 ${device.value}" } ?: "，未通过认证"),
            )
        }
    }

    private suspend fun DefaultWebSocketServerSession.performHandshake(): DeviceIdentifier? {
        if (!receiveConnectionRequest()) {
            return null
        }
        val authenticatedDevice = receiveAuthenticationRequest()
        if (authenticatedDevice == null) {
            return null
        }
        sendSignal(RemoteConnectionCode.AuthenticationSucceeded)
        logSink("事件通道认证通过：设备 ${authenticatedDevice.value}")
        return authenticatedDevice
    }

    private suspend fun DefaultWebSocketServerSession.receiveConnectionRequest(): Boolean {
        val clientMessage = receiveClientMessage() ?: return false
        if (clientMessage.messageType != RemoteMessageType.Connect) {
            logSink("事件通道握手第一步收到的不是建立连接消息，类别为 ${clientMessage.messageType}")
            sendSignal(RemoteConnectionCode.AuthenticationRequired)
            return false
        }
        if (clientMessage.protocolVersion != RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION) {
            logSink(
                "事件通道握手被拒：客户端协议版本 ${clientMessage.protocolVersion}，" +
                    "本插件只支持 ${RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION}",
            )
            sendSignal(RemoteConnectionCode.ProtocolVersionUnsupported)
            return false
        }
        return true
    }

    private suspend fun DefaultWebSocketServerSession.receiveAuthenticationRequest(): DeviceIdentifier? {
        val clientMessage = receiveClientMessage() ?: return null
        if (clientMessage.messageType != RemoteMessageType.Authenticate) {
            logSink("事件通道握手第二步收到的不是认证消息，类别为 ${clientMessage.messageType}")
            sendSignal(RemoteConnectionCode.AuthenticationRequired)
            return null
        }
        val authenticatedDevice = controlGate.resolveAuthenticatedDevice(clientMessage.deviceIdentifier)
        if (authenticatedDevice == null) {
            // 身份原文要写出来：插件重启或扩展重载之后登记处会清空，操作者得据此比对并重扫一张票据。
            logSink(
                "事件通道认证被拒：设备 ${clientMessage.deviceIdentifier?.value ?: "未提交身份"} 不在已配对登记处",
            )
            sendSignal(RemoteConnectionCode.AuthenticationFailed)
            return null
        }
        return authenticatedDevice
    }

    private suspend fun DefaultWebSocketServerSession.awaitResumeAndStreamEvents() {
        val usableSequenceNumber = awaitUsableResumeRequest()
        if (usableSequenceNumber != null) {
            streamEventsAfter(usableSequenceNumber)
        }
    }

    // 序号区间已不可用时不下线：客户端取过快照会再发一次 RESUME，这条会话要留在原地等它。
    private suspend fun DefaultWebSocketServerSession.awaitUsableResumeRequest(): Long? {
        while (true) {
            val clientMessage = receiveClientMessage() ?: return null
            val requestedSequenceNumber =
                if (clientMessage.messageType == RemoteMessageType.Resume) {
                    clientMessage.sequenceNumber
                } else {
                    null
                }

            if (requestedSequenceNumber != null) {
                if (canResumeFrom(requestedSequenceNumber)) {
                    return requestedSequenceNumber
                }
                logSink(
                    "事件通道的续传基准不可用：客户端声明已收到 $requestedSequenceNumber，" +
                        "本插件当前可回放 " +
                        "${eventStream.earliestAvailableSequenceNumber()}..${eventStream.latestSequenceNumber()}",
                )
                sendSignal(RemoteConnectionCode.EventsNoLongerAvailable)
                sendSignal(RemoteConnectionCode.SnapshotRequired)
            }
        }
    }

    private fun canResumeFrom(requestedSequenceNumber: Long): Boolean =
        requestedSequenceNumber >= eventStream.earliestAvailableSequenceNumber() - EARLIEST_SEQUENCE_OFFSET &&
            requestedSequenceNumber <= eventStream.latestSequenceNumber()

    // 只按序号过滤，不自己造事件：客户端看到的每一条都来自事件日志。
    //
    // 同时读 incoming：客户端发来的 Close 帧（以及 TCP 断开导致的通道关闭）必须立刻终止这条会话，
    // 否则事件流这一路 collect 会一直挂着，连接名额和设备关联要等 ping 超时（最多 30s）才释放——
    // 表现就是「手机都关了，插件里还显示在线」。
    private suspend fun DefaultWebSocketServerSession.streamEventsAfter(sequenceNumber: Long) {
        coroutineScope {
            val eventStreamingJob =
                launch {
                    eventStream.observeEvents()
                        .filter { event -> event.sequenceNumber > sequenceNumber }
                        .collect { event -> sendEvent(event) }
                }
            val incomingReaderJob =
                launch {
                    for (frame in incoming) {
                        // 只关心 Close 帧；其余控制帧（Ping/Pong）由引擎层处理，这里跳过。
                        if (frame is Frame.Close) break
                    }
                }
            // 任一路结束就取消另一路：Close 帧到了、TCP 断了、发送失败了，都该让会话立刻收场。
            incomingReaderJob.invokeOnCompletion { eventStreamingJob.cancel() }
            eventStreamingJob.invokeOnCompletion { incomingReaderJob.cancel() }
        }
    }

    private suspend fun DefaultWebSocketServerSession.receiveClientMessage(): RemoteClientMessage? {
        val frame = incoming.receiveCatching().getOrNull() ?: return null
        if (frame !is Frame.Text) {
            return null
        }
        return try {
            RemoteProtocolJson.instance.decodeFromString(RemoteClientMessage.serializer(), frame.readText())
        } catch (expectedMalformedMessage: SerializationException) {
            // 客户端发来的字节不可信：解析失败按协议错误处理，不能让它变成会话协程上的未捕获异常。
            // 原文不写进日志（可能带着配对码），但这件事必须留痕：否则握手会静默中断，两端都无线索。
            logSink("事件通道收到解不开的报文，按协议不一致收场")
            null
        }
    }

    private suspend fun DefaultWebSocketServerSession.sendEvent(event: RemoteEventEnvelope) {
        send(Frame.Text(RemoteProtocolJson.instance.encodeToString(RemoteEventEnvelope.serializer(), event)))
    }

    private suspend fun DefaultWebSocketServerSession.sendSignal(code: RemoteConnectionCode) {
        val signal =
            RemoteConnectionSignal(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                code = code,
            )
        send(Frame.Text(RemoteProtocolJson.instance.encodeToString(RemoteConnectionSignal.serializer(), signal)))
    }

    private companion object {
        private const val EVENTS_PATH = "/v1/events"

        // 帧上限原文未定义，此处选定值：256 KiB。
        // 依据：事件只带元数据，正常远小于此；给足余量是为了让携带小载荷的事件不被误伤。
        private const val MAXIMUM_FRAME_SIZE_BYTES = 262_144L

        private const val PING_PERIOD_MILLISECONDS = 15_000L

        private const val SESSION_TIMEOUT_MILLISECONDS = 15_000L

        // 已收到的最后一个序号合法时，可续传下界是「最早可用序号 - 1」。
        private const val EARLIEST_SEQUENCE_OFFSET = 1L

        private const val CONNECTION_LIMIT_REASON = "连接数已达上限"
    }
}
