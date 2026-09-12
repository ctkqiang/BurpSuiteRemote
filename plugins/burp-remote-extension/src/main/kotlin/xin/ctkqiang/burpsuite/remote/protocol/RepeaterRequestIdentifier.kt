// 一次 Repeater 请求的身份类型。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识一次送往 Repeater 的请求，把「创建」和「执行」两步串起来。
 *
 * @property value 文本取值。
 */
@JvmInline
@Serializable
value class RepeaterRequestIdentifier(val value: String)
