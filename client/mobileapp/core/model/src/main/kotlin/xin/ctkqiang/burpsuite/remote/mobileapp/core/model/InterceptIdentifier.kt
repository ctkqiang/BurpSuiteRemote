// 被拦截项的身份类型。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 标识一个被拦截项，生命周期只到它被放行或丢弃为止。
 *
 * @property value 文本取值。
 */
@JvmInline
value class InterceptIdentifier(val value: String)
