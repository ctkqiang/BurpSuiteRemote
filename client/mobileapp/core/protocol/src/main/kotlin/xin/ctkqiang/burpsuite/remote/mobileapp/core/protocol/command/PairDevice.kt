// 请求完成设备配对。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.DeviceIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.PairingChallengeIdentifierSerializer

/**
 * 请求完成配对；设备身份由移动端确立（plan §55），plan 未定义其余载荷（配对码怎么回传原文没写）。
 *
 * @property deviceIdentifier 移动端确立的设备身份。
 * @property pairingChallengeIdentifier 二维码里那一次配对尝试的身份。
 * @property operationIdentifier 本次执行的身份。
 */
@Serializable
data class PairDevice(
    @Serializable(with = DeviceIdentifierSerializer::class)
    val deviceIdentifier: DeviceIdentifier,
    @Serializable(with = PairingChallengeIdentifierSerializer::class)
    val pairingChallengeIdentifier: PairingChallengeIdentifier,
    @Serializable(with = OperationIdentifierSerializer::class)
    override val operationIdentifier: OperationIdentifier,
) : RemoteCommand
