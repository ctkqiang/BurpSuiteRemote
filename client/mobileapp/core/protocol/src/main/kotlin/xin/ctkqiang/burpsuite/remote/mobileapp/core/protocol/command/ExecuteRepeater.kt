// 请求执行 Repeater 请求。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.RepeaterRequestIdentifierSerializer

/**
 * 请求执行一个已创建的 Repeater 请求；plan 未定义其余载荷，因此只带标识符。
 *
 * @property repeaterRequestIdentifier 要执行的 Repeater 请求。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class ExecuteRepeater(
    @Serializable(with = RepeaterRequestIdentifierSerializer::class)
    val repeaterRequestIdentifier: RepeaterRequestIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
