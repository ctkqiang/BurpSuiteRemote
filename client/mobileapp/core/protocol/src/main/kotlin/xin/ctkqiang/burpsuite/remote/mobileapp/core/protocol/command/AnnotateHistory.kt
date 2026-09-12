// 请求为历史记录加注解。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.HistoryIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 请求给一条历史记录加注解；plan 未定义注解文本，因此只带标识符。
 *
 * @property historyIdentifier 要加注解的历史记录。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class AnnotateHistory(
    @Serializable(with = HistoryIdentifierSerializer::class)
    val historyIdentifier: HistoryIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
