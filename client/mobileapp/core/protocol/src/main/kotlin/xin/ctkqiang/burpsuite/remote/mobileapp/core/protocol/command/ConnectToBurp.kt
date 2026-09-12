// 请求与插件建立连接。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 请求与插件建立连接；plan 未定义其余载荷（地址与端口来自配对票据，不随命令回传）。
 *
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class ConnectToBurp(
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
