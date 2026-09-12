package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState

/**
 * 远程控制端口。
 *
 * 领域层不知道背后是 REST 加 WebSocket 还是进程内假实现（rules.md §6.2）；
 * 事件只此一个入口，客户端绝不自己造事件（rules.md §5.1）。
 */
interface RemoteControlClient {
    /** 连接状态；界面读它，不去读传输层的内部状态。 */
    val connectionState: Flow<ConnectionState>

    /** 当前会话收到的原始事件；未连接时是空流。 */
    fun observeEvents(): Flow<JournalEvent>

    /** 建连并保持；本方法返回即表示会话结束（用户断开或不再重连）。 */
    suspend fun connect(configuration: RemoteConnectionConfiguration)

    /** 主动断开；未连接时什么都不做。 */
    suspend fun disconnect()

    /** 配对：把票据里的一次性配对码换成设备身份。 */
    suspend fun pair(pairingAttempt: PairingAttempt): RemoteResult<DeviceIdentifier>

    /** 读取插件运行态，对应 GET /v1/status。 */
    suspend fun readRuntimeState(configuration: RemoteConnectionConfiguration): RemoteResult<RemoteRuntimeState>

    /** 读取插件自报能力，对应 GET /v1/capabilities。 */
    suspend fun readCapabilities(configuration: RemoteConnectionConfiguration): RemoteResult<ServerCapabilities>

    /** 读取远端历史列表原文，对应 GET /v1/history。 */
    suspend fun readRemoteHistory(configuration: RemoteConnectionConfiguration): RemoteResult<RemotePayload>

    /** 按标识读取远端历史记录原文，对应 GET /v1/history/{historyIdentifier}。 */
    suspend fun readRemoteHistoryMessage(
        configuration: RemoteConnectionConfiguration,
        historyIdentifier: HistoryIdentifier,
    ): RemoteResult<RemotePayload>

    /** 取当前状态快照，对应 GET /v1/snapshot；成功后本地续传基准一并推到快照那一刻。 */
    suspend fun requestSnapshot(configuration: RemoteConnectionConfiguration): RemoteResult<RemoteRuntimeState>
}
