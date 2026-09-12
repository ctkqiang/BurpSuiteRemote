package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 事件通道上的控制信号码；取值与插件侧 RemoteConnectionCode 的线上写法一致。 */
@Serializable
enum class RemoteConnectionSignalCode {
    /** 需要先完成认证。 */
    @SerialName("authentication_required")
    AuthenticationRequired,

    /** 认证通过，可以提交续传请求。 */
    @SerialName("authentication_succeeded")
    AuthenticationSucceeded,

    /** 认证失败，身份不被承认。 */
    @SerialName("authentication_failed")
    AuthenticationFailed,

    /** 协议版本不受支持。 */
    @SerialName("protocol_version_unsupported")
    ProtocolVersionUnsupported,

    /** 请求的序号区间已不在插件事件日志里，无法续传。 */
    @SerialName("events_no_longer_available")
    EventsNoLongerAvailable,

    /** 要求客户端先取一份当前状态快照再续传。 */
    @SerialName("snapshot_required")
    SnapshotRequired,
}
