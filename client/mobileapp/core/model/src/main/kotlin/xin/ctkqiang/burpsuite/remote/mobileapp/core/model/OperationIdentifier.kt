// 一次命令执行的身份类型。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 标识一次命令执行，同一个值只允许产生一次副作用，重试靠它幂等。
 *
 * @property value 文本取值。
 */
@JvmInline
value class OperationIdentifier(val value: String)
