// 事件归属的聚合类型。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 事件所属的聚合类型。种类压得很小，只有真需要按序重放的状态迁移才值得建一个聚合。 */
@Serializable
enum class AggregateType {
    /** 设备聚合。 */
    @SerialName("device")
    Device,

    /** HTTP 历史记录聚合。 */
    @SerialName("history")
    History,

    /** 拦截项聚合。 */
    @SerialName("intercept")
    Intercept,

    /** Repeater 请求聚合。 */
    @SerialName("repeater_request")
    RepeaterRequest,

    /** 截图聚合。 */
    @SerialName("screenshot")
    Screenshot,

    /** 远程会话聚合。 */
    @SerialName("remote_session")
    RemoteSession,
}
