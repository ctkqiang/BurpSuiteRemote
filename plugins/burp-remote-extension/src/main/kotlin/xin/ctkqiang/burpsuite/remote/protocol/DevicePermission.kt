// 已配对设备的权限等级。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 已配对设备的权限等级。只分只读和可控两档，先用一个能正确实现的模型顶住。 */
@Serializable
enum class DevicePermission {
    /** 只读。 */
    @SerialName("read")
    Read,

    /** 可控。 */
    @SerialName("control")
    Control,
}
