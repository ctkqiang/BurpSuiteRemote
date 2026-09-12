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
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventIngestor
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.PairingAttempt
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ReconnectionPolicy
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
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
    webSocketHttpClient: HttpClient = RemoteHttpClientFactory.create(),
) : RemoteControlClient {
    private val connectionStateFlow = MutableStateFlow(ConnectionState.Disconnected)

    private val eventListeners = CopyOnWriteArrayList<(JournalEvent) -> Unit>()

    private val lifecycleLock = Any()

    private var hasActiveConnection = false

    private var activeConnectionScope: CoroutineScope? = null

    private val eventStreamSession =
        RemoteEventStreamSession(
            httpClient = webSocketHttpClient,
            journalEventIngestor = journalEventIngestor,
            resumptionSequenceNumberProvider = resumptionSequenceNumberProvider,
            resynchronisation = resynchronisation,
            timeouts = timeouts,
            onEventReceived = ::publishEvent,
            onConnectionStateChanged = ::publishConnectionState,
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
        if (!beginConnection()) return
        val parentContext = currentCoroutineContext()
        val connectionScope = CoroutineScope(parentContext + SupervisorJob(parentContext[Job]))
        synchronized(lifecycleLock) { activeConnectionScope = connectionScope }
        try {
            connectionScope.launch { runReconnectionLoop(configuration) }.join()
        } finally {
            synchronized(lifecycleLock) {
                activeConnectionScope = null
                hasActiveConnection = false
            }
            connectionScope.cancel()
            publishConnectionState(ConnectionState.Disconnected)
        }
    }

    override suspend fun disconnect() {
        val connectionScope = synchronized(lifecycleLock) { activeConnectionScope }
        connectionScope?.cancel()
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
    ): RemoteResult<RemotePayload> = restClient.readRemoteHistoryMessage(configuration, historyIdentifier)

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
        connectionStateFlow.value = state
    }

    private fun publishEvent(event: JournalEvent) {
        for (listener in eventListeners) {
            listener(event)
        }
    }

    private fun beginConnection(): Boolean {
        synchronized(lifecycleLock) {
            if (hasActiveConnection) return false
            hasActiveConnection = true
        }
        return true
    }
}
