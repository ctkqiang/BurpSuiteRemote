// 线上消息的类别。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 线上消息的类别。是封闭集合，收到不认识的类别要明确拒绝，别静默丢掉。 */
@Serializable
enum class RemoteMessageType {
    /** 命令消息。 */
    @SerialName("command")
    Command,

    /** 查询消息。 */
    @SerialName("query")
    Query,

    /** 状态快照消息。 */
    @SerialName("snapshot")
    Snapshot,

    /** 事件消息。 */
    @SerialName("event")
    Event,

    /** 断点续传消息。 */
    @SerialName("resume")
    Resume,
}
