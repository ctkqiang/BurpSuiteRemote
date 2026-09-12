// 二维码承载的配对票据。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 一次配对所需的全部信息，也就是二维码里真正编码的内容。
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
    val challengeIdentifier: PairingChallengeIdentifier,
    val pairingCode: PairingCode,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    val expiresAt: Instant,
)
