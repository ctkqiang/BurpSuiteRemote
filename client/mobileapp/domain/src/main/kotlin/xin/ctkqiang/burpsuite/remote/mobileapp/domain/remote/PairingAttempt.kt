package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode

/**
 * 一次配对尝试：二维码里解出来的地址、会话身份与一次性配对码。
 */
data class PairingAttempt(
    /** 插件所在主机地址。 */
    val host: String,
    /** 插件监听端口。 */
    val port: Int,
    /** 本次配对尝试的会话身份。 */
    val challengeIdentifier: PairingChallengeIdentifier,
    /** 一次性配对码。 */
    val pairingCode: PairingCode,
    /** 是否使用 TLS 方案；与非配对调用保持同一口径。 */
    val isTlsEnabled: Boolean = false,
)
