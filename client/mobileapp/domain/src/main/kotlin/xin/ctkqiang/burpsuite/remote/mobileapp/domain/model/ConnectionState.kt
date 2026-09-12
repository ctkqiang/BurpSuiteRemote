package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

/** 远程会话的连接状态；取值来自 plan §60 的状态机，界面照它决定此刻能显示什么。 */
enum class ConnectionState {
    /** 尚未连接，也没有在尝试连接。 */
    Disconnected,

    /** 正在寻找插件端点。 */
    Discovering,

    /** 正在建立连接。 */
    Connecting,

    /** 正在认证设备身份。 */
    Authenticating,

    /** 已连上，正在按序号补齐事件。 */
    Synchronising,

    /** 连接可用，事件流正常。 */
    Connected,

    /** 掉线后正在按退避策略重连。 */
    Reconnecting,

    /** 插件已不保留所需序号区间，取快照后重新续传。 */
    ResynchronisationRequired,

    /** 认证被插件拒绝。 */
    AuthenticationFailed,

    /** 双方协议版本或其他线上契约不一致。 */
    ProtocolError,

    /** 握手或请求超时。 */
    TimedOut,

    /** 端点不可达。 */
    ServerUnavailable,
    ;

    /** 会话此刻是否真的可用；只有 Connected 算可用，其余都是过渡态或故障态。 */
    val isConnected: Boolean get() = this == Connected
}
