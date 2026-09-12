// 二维码承载的配对票据。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EpochMillisecondsInstantSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.PairingChallengeIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.PairingCodeSerializer
import java.time.Instant

/**
 * 一次配对所需的全部信息，也就是二维码里真正编码的内容；字段顺序必须与插件侧编码器一致。
 *
 * @property protocolVersion 协议版本。
 * @property host 插件监听的地址。
 * @property port 插件监听的端口。
 * @property challengeIdentifier 本次配对尝试的身份。
 * @property pairingCode 一次性配对码。
 * @property expiresAt 失效时刻，毫秒整数。
 */
@Serializable
data class PairingTicket(
    val protocolVersion: Int,
    val host: String,
    val port: Int,
    @Serializable(with = PairingChallengeIdentifierSerializer::class)
    val challengeIdentifier: PairingChallengeIdentifier,
    @Serializable(with = PairingCodeSerializer::class)
    val pairingCode: PairingCode,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    val expiresAt: Instant,
)
