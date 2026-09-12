// 请求丢弃拦截项。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.InterceptIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 请求丢弃指定拦截项；丢弃后该拦截项不再存在，因此重试必须靠执行身份挡掉。
 *
 * @property interceptIdentifier 要丢弃的拦截项。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class DropIntercept(
    @Serializable(with = InterceptIdentifierSerializer::class)
    val interceptIdentifier: InterceptIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
