// 命令被有意拒绝的原因。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 命令被拒绝的原因。拒绝是预期内的领域结果，重试多少次都一样，所以不和失败混着报。 */
@Serializable
enum class RejectionReason {
    /** 设备尚未配对。 */
    @SerialName("device_not_paired")
    DeviceNotPaired,

    /** 设备权限不足。 */
    @SerialName("device_not_authorized")
    DeviceNotAuthorized,

    /** 超出速率限制。 */
    @SerialName("rate_limit_exceeded")
    RateLimitExceeded,

    /** 目标不可用。 */
    @SerialName("target_not_available")
    TargetNotAvailable,

    /** 协议版本不受支持。 */
    @SerialName("unsupported_protocol_version")
    UnsupportedProtocolVersion,

    /** 配对会话不存在。 */
    @SerialName("pairing_session_not_available")
    PairingSessionNotAvailable,

    /** 配对会话已过期。 */
    @SerialName("pairing_session_expired")
    PairingSessionExpired,

    /** 配对码不匹配。 */
    @SerialName("pairing_code_mismatch")
    PairingCodeMismatch,
}
