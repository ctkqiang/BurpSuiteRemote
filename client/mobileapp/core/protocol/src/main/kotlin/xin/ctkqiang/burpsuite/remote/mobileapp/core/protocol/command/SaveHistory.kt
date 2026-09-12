// 请求保存历史记录。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.HistoryIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 请求把一条历史记录存入本地归档；plan 未定义其余载荷，因此只带标识符。
 *
 * @property historyIdentifier 要保存的历史记录。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class SaveHistory(
    @Serializable(with = HistoryIdentifierSerializer::class)
    val historyIdentifier: HistoryIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
