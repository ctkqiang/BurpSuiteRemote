/**
 * Burp Remote —— 协议层 / 拒绝原因
 *
 * 声明命令被有意拒绝的原因。拒绝是预期内的领域结果，不是缺陷，因此它必须是一个
 * 明确的类型，而不是一句附在异常里的说明文字。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 命令被拒绝的原因。
 *
 * 「被拒绝」与「执行失败」是两件事，必须分开建模：拒绝意味着请求不合法或不被允许，
 * 重试一千次结果都一样；失败意味着环境出了问题，稍后重试可能成功。把两者混成一个
 * 错误字符串，客户端就只能靠字符串匹配来决定要不要重试，而这种判断迟早会出错。
 *
 * 每一项都是稳定的机器码，不含任何界面语言。插件不产出用户看得懂的文字，翻译由客户端
 * 依据这些码自行完成。
 */
@Serializable
enum class RejectionReason {
    /** 发起命令的设备尚未完成配对，插件不承认其身份。 */
    @SerialName("device_not_paired")
    DeviceNotPaired,

    /** 设备已配对，但其权限不足以执行该命令，例如只读设备试图放行拦截项。 */
    @SerialName("device_not_authorized")
    DeviceNotAuthorized,

    /** 命令触发了速率限制。远程控制属于高危操作，突发的大量命令必须被节流。 */
    @SerialName("rate_limit_exceeded")
    RateLimitExceeded,

    /** 命令指向的对象当前不存在或已不可操作，例如拦截项已被放行或丢弃。 */
    @SerialName("target_not_available")
    TargetNotAvailable,

    /** 报文的协议版本高于插件所能理解的上限，插件选择拒绝而不是尽力解析。 */
    @SerialName("unsupported_protocol_version")
    UnsupportedProtocolVersion,

    /**
     * 当前不存在可用的配对会话：尚未生成、已被消费，或已被新票据顶替。
     *
     * 三种情况合并成同一个码，而不是分别告知，是有意为之：把它们区分开，等于向局域网内
     * 的探测者确认「此刻插件正开着一个配对窗口」，而这正是他最希望知道的信息。
     */
    @SerialName("pairing_session_not_available")
    PairingSessionNotAvailable,

    /** 配对会话存在但已超过有效期。二维码在屏幕上停留过久时必须失效，而不是继续可用。 */
    @SerialName("pairing_session_expired")
    PairingSessionExpired,

    /** 提交的配对码与当前会话不匹配。 */
    @SerialName("pairing_code_mismatch")
    PairingCodeMismatch,
}
