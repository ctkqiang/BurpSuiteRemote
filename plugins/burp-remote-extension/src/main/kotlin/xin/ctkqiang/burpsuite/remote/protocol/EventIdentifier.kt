// 事件日志中「事件」的身份类型。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识事件日志里的一条事件，和序号一起组成客户端的去重键。
 *
 * @property value 文本取值。
 */
@JvmInline
@Serializable
value class EventIdentifier(val value: String)
