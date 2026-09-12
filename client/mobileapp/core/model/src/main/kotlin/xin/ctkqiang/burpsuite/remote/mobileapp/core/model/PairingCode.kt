// 一次性配对凭证。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 一次性配对码，用来证明配对请求的发起者确实看到了屏幕上的二维码。
 *
 * @property value 文本取值。
 */
@JvmInline
value class PairingCode(val value: String) {
    /** 脱敏输出；日志与崩溃报告都会调它，不能把明文带出去。 */
    override fun toString(): String = "PairingCode(已脱敏)"
}
