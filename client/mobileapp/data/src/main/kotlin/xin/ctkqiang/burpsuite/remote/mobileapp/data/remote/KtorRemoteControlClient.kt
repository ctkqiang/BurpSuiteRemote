package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventIngestor
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.PairingAttempt
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ReconnectionPolicy
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteHistoryMessage
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemotePayload
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteRuntimeState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteTimeouts
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ResumptionSequenceNumberProvider
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ServerCapabilities
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 远程控制端口的真实实现：REST 调用加事件通道，按 [ReconnectionPolicy] 重连。
 *
 * [connect] 会跑完整个会话生命周期，直到被 [disconnect] 停下或遇到不可自愈的故障（认证被拒、
 * 协议不一致）。连接状态与收到的原始事件都推给界面，界面据此显示 LIVE/OFFLINE（plan §59）。
 *
 * TLS 尚未实现：插件侧当前只提供明文端点，[RemoteConnectionConfiguration.isTlsEnabled] 打开只会改用
 * https/wss 方案，不代表链路已经加密（plan §54）。
 */
class KtorRemoteControlClient(
    private val restClient: RemoteRestClient,
    private val deviceIdentifierProvider: RemoteDeviceIdentifierProvider,
    journalEventIngestor: JournalEventIngestor,
    resumptionSequenceNumberProvider: ResumptionSequenceNumberProvider,
    resynchronisation: RemoteResynchronisation,
    private val reconnectionPolicy: ReconnectionPolicy = ReconnectionPolicy(),
    private val timeouts: RemoteTimeouts = RemoteTimeouts(),
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
    webSocketHttpClient: HttpClient = RemoteHttpClientFactory.create(),
) : RemoteControlClient {
    private val connectionStateFlow = MutableStateFlow(ConnectionState.Disconnected)

    private val eventListeners = CopyOnWriteArrayList<(JournalEvent) -> Unit>()

    private val lifecycleLock = Any()

    private var activeConnectionScope: CoroutineScope? = null

    // 每开一次会话就加一；旧会话收尾时靠它判断自己是否已被新会话取代，取代了就不能再动共享状态。
    private var connectionGeneration = 0L

    private val eventStreamSession =
        RemoteEventStreamSession(
            httpClient = webSocketHttpClient,
            journalEventIngestor = journalEventIngestor,
            resumptionSequenceNumberProvider = resumptionSequenceNumberProvider,
            resynchronisation = resynchronisation,
            timeouts = timeouts,
            onEventReceived = ::publishEvent,
            onConnectionStateChanged = ::publishConnectionState,
            technicalLog = technicalLog,
        )

    override val connectionState: Flow<ConnectionState>
        get() = connectionStateFlow.asStateFlow()

    override fun observeEvents(): Flow<JournalEvent> =
        callbackFlow {
            val listener: (JournalEvent) -> Unit = { event -> trySend(event) }
            eventListeners.add(listener)
            awaitClose { eventListeners.remove(listener) }
        }.buffer(Channel.BUFFERED)

    override suspend fun connect(configuration: RemoteConnectionConfiguration) {
        // 父上下文在临界区之外取：synchronized 里不允许有挂起点。
        val parentContext = currentCoroutineContext()
        val generation: Long
        val connectionScope: CoroutineScope
        synchronized(lifecycleLock) {
            // 再来一次 connect 就是换一次会话：旧会话必须让位。若像以前那样直接返回，扫码换到的新身份与
            // 新地址就永远等不到自己的连接，界面则一直停在 OFFLINE。
            connectionGeneration += 1
            generation = connectionGeneration
            activeConnectionScope?.cancel()
            connectionScope = CoroutineScope(parentContext + SupervisorJob(parentContext[Job]))
            activeConnectionScope = connectionScope
        }
        try {
            connectionScope.launch { runReconnectionLoop(configuration) }.join()
        } finally {
            synchronized(lifecycleLock) {
                // 已被新会话取代时什么都不动：否则旧会话收尾会把新会话的句柄抹掉。
                if (generation == connectionGeneration) activeConnectionScope = null
            }
            connectionScope.cancel()
        }
    }

    override suspend fun disconnect() {
        synchronized(lifecycleLock) {
            connectionGeneration += 1
            activeConnectionScope?.cancel()
            activeConnectionScope = null
        }
        // 用户主动断开是唯一会回到「未连接且不再尝试」的路径；故障必须停在故障态，
        // 否则认证失败、协议不一致都会被抹平成 OFFLINE，用户看不到任何可排查的原因。
        publishConnectionState(ConnectionState.Disconnected)
    }

    override suspend fun pair(pairingAttempt: PairingAttempt): RemoteResult<DeviceIdentifier> =
        restClient.pair(pairingAttempt)

    override suspend fun readRuntimeState(
        configuration: RemoteConnectionConfiguration,
    ): RemoteResult<RemoteRuntimeState> = restClient.readRuntimeState(configuration)

    override suspend fun readCapabilities(
        configuration: RemoteConnectionConfiguration,
    ): RemoteResult<ServerCapabilities> = restClient.readCapabilities(configuration)

    override suspend fun readRemoteHistory(configuration: RemoteConnectionConfiguration): RemoteResult<RemotePayload> =
        restClient.readRemoteHistory(configuration)

    override suspend fun readRemoteHistoryMessage(
        configuration: RemoteConnectionConfiguration,
        historyIdentifier: HistoryIdentifier,
    ): RemoteResult<RemoteHistoryMessage> = restClient.readRemoteHistoryMessage(configuration, historyIdentifier)

    override suspend fun requestSnapshot(
        configuration: RemoteConnectionConfiguration,
    ): RemoteResult<RemoteRuntimeState> = restClient.requestSnapshot(configuration)

    private suspend fun runReconnectionLoop(configuration: RemoteConnectionConfiguration) {
        var attemptNumber = 0
        while (true) {
            val deviceIdentifier = deviceIdentifierProvider.currentDeviceIdentifier()
            if (deviceIdentifier == null) {
                // 没有身份就无从认证；这不是可达性问题，重试不会变好，就此收场。
                publishConnectionState(ConnectionState.Disconnected)
                return
            }

            publishConnectionState(if (attemptNumber == 0) ConnectionState.Connecting else ConnectionState.Reconnecting)
            when (val sessionEnd = eventStreamSession.stream(configuration, deviceIdentifier)) {
                // 断洞：协调器已把基准留在缺口前一位，立刻重连续传，不必退避。
                RemoteSessionEnd.ResumeRequired -> attemptNumber = 0

                RemoteSessionEnd.PeerClosed,
                RemoteSessionEnd.TransportFailure,
                RemoteSessionEnd.ServerUnavailable,
                RemoteSessionEnd.HandshakeTimedOut,
                -> {
                    publishRetryState(sessionEnd)
                    attemptNumber += 1
                    delay(reconnectionPolicy.delayBeforeAttempt(attemptNumber))
                }

                RemoteSessionEnd.AuthenticationRejected -> {
                    publishConnectionState(ConnectionState.AuthenticationFailed)
                    return
                }

                RemoteSessionEnd.ProtocolMismatch -> {
                    publishConnectionState(ConnectionState.ProtocolError)
                    return
                }

                RemoteSessionEnd.ResynchronisationFailed -> {
                    publishConnectionState(ConnectionState.ResynchronisationRequired)
                    return
                }
            }
        }
    }

    private fun publishRetryState(sessionEnd: RemoteSessionEnd) {
        when (sessionEnd) {
            RemoteSessionEnd.ServerUnavailable -> publishConnectionState(ConnectionState.ServerUnavailable)
            RemoteSessionEnd.HandshakeTimedOut -> publishConnectionState(ConnectionState.TimedOut)
            else -> publishConnectionState(ConnectionState.Reconnecting)
        }
    }

    private fun publishConnectionState(state: ConnectionState) {
        // 连接状态是界面唯一读的东西，记下每一次跃迁，排查「为什么一直是 OFFLINE」时才有据可依。
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.EventStream,
                message = "连接状态变化",
                attributes = mapOf("connectionState" to state.name),
            ),
        )
        connectionStateFlow.value = state
    }

    private fun publishEvent(event: JournalEvent) {
        for (listener in eventListeners) {
            listener(event)
        }
    }
}
