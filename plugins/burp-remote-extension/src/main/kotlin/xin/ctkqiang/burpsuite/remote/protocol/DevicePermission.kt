/**
 * Burp Remote —— 协议层 / 设备权限
 *
 * 声明一台已配对设备可以获得的权限等级。远程控制 Burp 是一件危险的事，因此权限被拆成
 * 「只读」与「可控」两档，而不是一个笼统的「已授权」布尔值。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 已配对设备的权限等级。
 *
 * 这里只有两档，是有意为之：先让权限模型足够简单到能被正确实现，再考虑细分。一个
 * 「已授权」布尔值无法表达「你可以查看流量，但不许放行请求」这种真实需求，于是任何
 * 只想看数据的设备都不得不拿到完整控制权，攻击面被无谓放大。
 *
 * 权限判定必须发生在插件侧，且发生在执行命令之前。客户端自报的权限等级只是请求，
 * 不是事实。
 */
@Serializable
enum class DevicePermission {
    /** 只读权限：允许查询状态、历史、请求、响应以及订阅事件流，不允许改变任何状态。 */
    @SerialName("read")
    Read,

    /** 控制权限：允许修改并放行拦截项、向 Repeater 发送请求等具有副作用的操作。 */
    @SerialName("control")
    Control,
}
