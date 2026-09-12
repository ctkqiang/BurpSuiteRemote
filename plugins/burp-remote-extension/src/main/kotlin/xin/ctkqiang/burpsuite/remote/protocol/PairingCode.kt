/**
 * Burp Remote —— 协议层 / 配对
 *
 * 声明一次性配对凭证的类型。凭证必须由独立类型承载，而不能当作普通字符串传递：一旦它
 * 与其它字符串混用，就可能在日志、异常上下文或崩溃报告中顺带流出。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 一次性配对码，用于向插件证明配对请求的发起者确实看到了屏幕上的二维码。
 *
 * 该值等价于一次性的持有者凭证，因此 [toString] 被刻意改写为脱敏形式。调试日志、异常
 * 上下文与崩溃报告都会调用 `toString`，默认实现会把明文凭证一路带进这些输出，而
 * rules.md §12 明确禁止记录凭证。序列化不受影响：线路上仍是明文，否则移动端无法完成比对。
 *
 * @property value 配对码明文，仅由无歧义字符集构成，长度固定。
 */
@JvmInline
@Serializable
value class PairingCode(val value: String) {
    /**
     * 返回脱敏后的描述，任何情况下都不返回凭证明文。
     */
    override fun toString(): String = "PairingCode(已脱敏)"
}
