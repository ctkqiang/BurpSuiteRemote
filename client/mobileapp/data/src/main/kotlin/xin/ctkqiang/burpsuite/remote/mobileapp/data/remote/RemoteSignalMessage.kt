package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.Serializable

/**
 * 事件通道上的控制信号报文；messageType 恒为 signal，解码入口已按它分流。
 *
 * @property protocolVersion 插件使用的协议版本。
 * @property code 信号码。
 */
@Serializable
data class RemoteSignalMessage(val protocolVersion: Int, val code: RemoteConnectionSignalCode)
