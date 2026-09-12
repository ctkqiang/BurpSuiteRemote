// 请求放行拦截项。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.InterceptIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 请求放行指定拦截项；是否真的放行，要看随后收到的 InterceptForwarded 事件。
 *
 * @property interceptIdentifier 要放行的拦截项。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class ForwardIntercept(
    @Serializable(with = InterceptIdentifierSerializer::class)
    val interceptIdentifier: InterceptIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
