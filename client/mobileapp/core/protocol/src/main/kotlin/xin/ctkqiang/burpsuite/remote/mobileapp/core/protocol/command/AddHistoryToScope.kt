// 请求把历史记录的主机加入作用域。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.HistoryIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 请求把一条历史记录所属的主机加入 Burp 作用域。
 *
 * 只带标识符，不带主机串：主机串由插件读它当下的事实拼出，过期记录也不会把错误的地址写进作用域。
 *
 * @property historyIdentifier 决定加入哪个主机的那条历史记录。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class AddHistoryToScope(
    @Serializable(with = HistoryIdentifierSerializer::class)
    val historyIdentifier: HistoryIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
