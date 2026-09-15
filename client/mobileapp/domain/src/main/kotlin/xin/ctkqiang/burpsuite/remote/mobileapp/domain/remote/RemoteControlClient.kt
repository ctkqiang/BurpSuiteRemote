package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.RemoteCommand
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

    /** 按标识取回单条历史报文的完整内容，对应 GET /v1/history/{historyIdentifier}。 */
    suspend fun readRemoteHistoryMessage(
        configuration: RemoteConnectionConfiguration,
        historyIdentifier: HistoryIdentifier,
    ): RemoteResult<RemoteHistoryMessage>

    /** 取当前状态快照，对应 GET /v1/snapshot；成功后本地续传基准一并推到快照那一刻。 */
    suspend fun requestSnapshot(configuration: RemoteConnectionConfiguration): RemoteResult<RemoteRuntimeState>

    /**
     * 为一条即将发出的命令取一个新的执行身份。
     *
     * 身份格式是两端协议契约（rules.md §11），发令方不自己编，只从这一个入口取；
     * 取到之后必须原样放进命令对象，重试时复用同一个值，插件据此去重（rules.md §5.5）。
     */
    fun nextOperationIdentifier(): OperationIdentifier

    /**
     * 执行一条控制命令。
     *
     * 命令类型不出现在报文中（rules.md §11）：命令对象决定打到插件哪个端点，插件按端点分派；
     * 命令自带的执行身份原样上行，因此重试不会产生第二次副作用。插件尚未提供该端点时，
     * 结果如实为 [RemoteFailure.ActionNotSupported]，界面才知道该说「服务端还没做」。
     */
    suspend fun dispatch(
        configuration: RemoteConnectionConfiguration,
        command: RemoteCommand,
    ): RemoteResult<Unit>

    /**
     * 执行一条带请求体的控制命令。
     *
     * 与 [dispatch] 同路，但额外把 [requestBody] 作为 HTTP 正文发出去。插件端某些端点
     * （例如 Repeater create、Intercept modify）从请求体里读取 `requestText` 等业务字段，
     * 没有请求体时插件回 `SerializationFailure`，因此必须走这一条而不是裸 [dispatch]。
     *
     * 命令对象仍然决定端点路径与操作身份；[requestBody] 只承载插件端点要读的那些字段。
     */
    suspend fun dispatchWithBody(
        configuration: RemoteConnectionConfiguration,
        command: RemoteCommand,
        requestBody: String,
    ): RemoteResult<Unit>
}
