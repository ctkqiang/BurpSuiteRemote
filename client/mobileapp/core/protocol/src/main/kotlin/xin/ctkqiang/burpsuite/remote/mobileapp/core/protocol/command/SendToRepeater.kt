// 请求把历史记录送往 Repeater。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.HistoryIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 请求把一条历史记录送往 Repeater；plan 未定义其余载荷，因此只带标识符。
 *
 * @property historyIdentifier 要送往 Repeater 的历史记录。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class SendToRepeater(
    @Serializable(with = HistoryIdentifierSerializer::class)
    val historyIdentifier: HistoryIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
