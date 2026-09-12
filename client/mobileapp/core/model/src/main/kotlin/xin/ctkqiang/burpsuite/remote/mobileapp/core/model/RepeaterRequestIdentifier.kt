// 一次 Repeater 请求的身份类型。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 标识一次送往 Repeater 的请求，把「创建」和「执行」两步串起来。
 *
 * @property value 文本取值。
 */
@JvmInline
value class RepeaterRequestIdentifier(val value: String)
