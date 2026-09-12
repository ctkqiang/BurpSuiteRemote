// 请求为历史记录加书签。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.HistoryIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 请求为一条历史记录加书签；plan 未定义其余载荷，因此只带标识符。
 *
 * @property historyIdentifier 要加书签的历史记录。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class BookmarkHistory(
    @Serializable(with = HistoryIdentifierSerializer::class)
    val historyIdentifier: HistoryIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
