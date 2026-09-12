// WebSocket 会话上的控制信号报文。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 插件在事件通道上发出的控制信号。
 *
 * 和事件信封分开：事件是事实，信号是传输层状态，两者的字段集合没有交集，硬塞进一个类型会让客户端多出一半用不上的字段。
 *
 * @property protocolVersion 插件使用的协议版本。
 * @property messageType 固定为 [RemoteMessageType.Signal]；保留字段是为了让每条线上报文都带消息类别。
 * @property code 信号码。
 */
@Serializable
data class RemoteConnectionSignal(
    val protocolVersion: Int,
    val messageType: RemoteMessageType = RemoteMessageType.Signal,
    val code: RemoteConnectionCode,
)
