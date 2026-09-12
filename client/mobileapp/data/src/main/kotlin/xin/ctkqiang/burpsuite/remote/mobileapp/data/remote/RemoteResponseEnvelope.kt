package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.CommandResult

/**
 * 插件对 REST 请求的统一应答信封，字段与插件侧 RemoteResponse 一一对应。
 *
 * messageType 按文本承接：插件侧是枚举，客户端不认识新版类别时也不该让整条应答解析失败。
 *
 * @property protocolVersion 插件使用的协议版本。
 * @property messageType 消息类别。
 * @property result 命令类请求的结局；查询类请求成功时为 null。
 * @property payload 查询类请求成功时的数据；失败时为 null。
 */
@Serializable
data class RemoteResponseEnvelope(
    val protocolVersion: Int,
    val messageType: String,
    val result: CommandResult? = null,
    val payload: JsonElement? = null,
)
