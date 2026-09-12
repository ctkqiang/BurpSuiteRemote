package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.EventEnvelope
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.EventIngestionOutcome
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventIngestor
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteTimeouts
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ResumptionSequenceNumberProvider
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

// 单条事件通道会话：握手、认证、续传、收事件，必要时取快照再续传。
internal class RemoteEventStreamSession(
    private val httpClient: HttpClient,
    private val journalEventIngestor: JournalEventIngestor,
    private val resumptionSequenceNumberProvider: ResumptionSequenceNumberProvider,
    private val resynchronisation: RemoteResynchronisation,
    private val timeouts: RemoteTimeouts,
    private val onEventReceived: (JournalEvent) -> Unit,
    private val onConnectionStateChanged: (ConnectionState) -> Unit,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    /**
     * 跑完一条会话；返回即表示这条会话已经结束，由调用方决定重连还是上报故障。
     *
     * 握手顺序固定为 CONNECT → AUTHENTICATE → RESUME：插件在认证之前不推送任何事件，
     * 提前发 RESUME 只会被当成一次顺序错误的握手。
     */
    suspend fun stream(
        configuration: RemoteConnectionConfiguration,
        deviceIdentifier: DeviceIdentifier,
    ): RemoteSessionEnd {
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.EventStream,
                message = "打开事件通道",
                attributes = mapOf("host" to configuration.host, "port" to configuration.port.toString()),
            ),
        )
        val sessionEnd = runSession(configuration, deviceIdentifier)
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.EventStream,
                message = "事件通道会话结束",
                attributes = mapOf("reason" to sessionEnd.name),
            ),
        )
        return sessionEnd
    }

    private suspend fun runSession(
        configuration: RemoteConnectionConfiguration,
        deviceIdentifier: DeviceIdentifier,
    ): RemoteSessionEnd {
        var sessionEnd = RemoteSessionEnd.PeerClosed
        try {
            httpClient.webSocket(eventsUrl(configuration)) {
                val handshakeConclusion =
                    withTimeout(timeouts.handshakeMilliseconds) { performHandshake(deviceIdentifier) }
                sessionEnd =
                    when (handshakeConclusion) {
                        RemoteHandshakeConclusion.Authenticated -> {
                            sendResume()
                            streamEvents(configuration)
                        }

                        RemoteHandshakeConclusion.AuthenticationRejected -> {
                            close(CloseReason(CloseReason.Codes.NORMAL, CLOSE_REASON_AUTHENTICATION_REJECTED))
                            RemoteSessionEnd.AuthenticationRejected
                        }

                        RemoteHandshakeConclusion.ProtocolMismatch -> {
                            close(CloseReason(CloseReason.Codes.NORMAL, CLOSE_REASON_PROTOCOL_MISMATCH))
                            RemoteSessionEnd.ProtocolMismatch
                        }

                        RemoteHandshakeConclusion.PeerClosed -> RemoteSessionEnd.PeerClosed
                    }
            }
        } catch (handshakeTimeout: TimeoutCancellationException) {
            return RemoteSessionEnd.HandshakeTimedOut
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (unreachableServer: ConnectException) {
            return RemoteSessionEnd.ServerUnavailable
        } catch (unresolvableHost: UnknownHostException) {
            return RemoteSessionEnd.ServerUnavailable
        } catch (transportFailure: IOException) {
            return RemoteSessionEnd.TransportFailure
        } catch (unexpectedFailure: Exception) {
            return RemoteSessionEnd.TransportFailure
        }
        return sessionEnd
    }

    // 收到达成结论的报文就收场；其余报文（认证前的事件、与握手无关的信号）按噪声跳过。
    private suspend fun DefaultClientWebSocketSession.performHandshake(
        deviceIdentifier: DeviceIdentifier,
    ): RemoteHandshakeConclusion {
        sendMessage(
            RemoteClientMessage(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                messageType = RemoteClientMessageType.Connect,
            ),
        )
        sendMessage(
            RemoteClientMessage(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                messageType = RemoteClientMessageType.Authenticate,
                deviceIdentifier = deviceIdentifier,
            ),
        )

        while (true) {
            val frame = incoming.receiveCatching().getOrNull() ?: return RemoteHandshakeConclusion.PeerClosed
            if (frame is Frame.Close) return RemoteHandshakeConclusion.PeerClosed
            if (frame !is Frame.Text) continue
            val conclusion =
                RemoteHandshakeConclusion.forInboundMessage(RemoteInboundMessageDecoder.decode(frame.readText()))
            if (conclusion != null) return conclusion
        }
    }

    private suspend fun DefaultClientWebSocketSession.streamEvents(
        configuration: RemoteConnectionConfiguration,
    ): RemoteSessionEnd {
        onConnectionStateChanged(ConnectionState.Connected)
        while (true) {
            val frame = incoming.receiveCatching().getOrNull() ?: return RemoteSessionEnd.PeerClosed
            if (frame is Frame.Close) return RemoteSessionEnd.PeerClosed
            if (frame !is Frame.Text) continue
            when (val inbound = RemoteInboundMessageDecoder.decode(frame.readText())) {
                RemoteInboundMessage.Malformed -> return RemoteSessionEnd.ProtocolMismatch

                is RemoteInboundMessage.EventReceived -> {
                    val sessionEnd = RemoteEventIngestionDisposition.sessionEndFor(ingest(inbound.envelope))
                    if (sessionEnd != null) return sessionEnd
                }

                is RemoteInboundMessage.SignalReceived ->
                    when (val disposition = RemoteSignalDisposition.forSignalCode(inbound.code)) {
                        RemoteSignalDisposition.Continue -> Unit

                        // 插件在等新的 RESUME；快照取不到就没法继续这次会话，如实收场。
                        RemoteSignalDisposition.ResynchroniseThenResume -> {
                            if (resynchronisation.resynchronise(configuration) is RemoteResult.Failed) {
                                return RemoteSessionEnd.ResynchronisationFailed
                            }
                            onConnectionStateChanged(ConnectionState.Synchronising)
                            sendResume()
                            onConnectionStateChanged(ConnectionState.Connected)
                        }

                        is RemoteSignalDisposition.EndSession -> return disposition.sessionEnd
                    }
            }
        }
    }

    // 事件先给观察者再入账：观察者要的是「此次会话收到的原始事件」，去重与断洞是日志的事。
    private suspend fun ingest(envelope: EventEnvelope): EventIngestionOutcome {
        val journalEvent = RemoteJournalEventMapper.map(envelope)
        onEventReceived(journalEvent)
        return journalEventIngestor.ingest(journalEvent)
    }

    private suspend fun DefaultClientWebSocketSession.sendResume() {
        val resumeAfterSequenceNumber = resumptionSequenceNumberProvider.currentSequenceNumber()
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.EventStream,
                message = "提交续传基准",
                attributes = mapOf("resumeAfterSequenceNumber" to resumeAfterSequenceNumber.toString()),
            ),
        )
        sendMessage(RemoteResumeRequest.build(resumeAfterSequenceNumber))
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: RemoteClientMessage) {
        send(Frame.Text(RemoteWireJson.instance.encodeToString(RemoteClientMessage.serializer(), message)))
    }

    private fun eventsUrl(configuration: RemoteConnectionConfiguration): String {
        val scheme = if (configuration.isTlsEnabled) SECURE_WEB_SOCKET_SCHEME else WEB_SOCKET_SCHEME
        return "$scheme://${configuration.host}:${configuration.port}${RemoteEndpointPath.EVENTS}"
    }

    private companion object {
        const val WEB_SOCKET_SCHEME = "ws"
        const val SECURE_WEB_SOCKET_SCHEME = "wss"
        const val CLOSE_REASON_AUTHENTICATION_REJECTED = "设备身份被拒"
        const val CLOSE_REASON_PROTOCOL_MISMATCH = "协议不一致"
    }
}
