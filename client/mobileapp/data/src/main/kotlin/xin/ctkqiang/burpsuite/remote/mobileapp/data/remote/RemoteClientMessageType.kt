package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 客户端发往插件的消息类别；取值与插件侧 RemoteMessageType 的线上写法一致。 */
@Serializable
enum class RemoteClientMessageType {
    /** 建立连接，事件通道握手第一步。 */
    @SerialName("connect")
    Connect,

    /** 提交设备身份，事件通道握手第二步。 */
    @SerialName("authenticate")
    Authenticate,

    /** 声明已收到的最后一个序号，事件通道握手第三步。 */
    @SerialName("resume")
    Resume,

    /** 提交一次性配对码换取设备身份。 */
    @SerialName("pair")
    Pair,
}
