// 插件对 REST 请求的统一应答。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * REST 应答的统一信封。
 *
 * 成功与失败都用同一个形状返回，客户端因此只需要一条解析路径；领域失败放在 [result] 里，传输级失败才用 HTTP 状态码。
 *
 * @property protocolVersion 插件使用的协议版本。
 * @property messageType 这条应答对应的消息类别。
 * @property result 命令类请求的结局；查询类请求成功时为 null。
 * @property payload 查询类请求成功时的数据；失败时为 null。
 */
@Serializable
data class RemoteResponse(
    val protocolVersion: Int,
    val messageType: RemoteMessageType,
    val result: CommandResult? = null,
    val payload: JsonElement? = null,
)
