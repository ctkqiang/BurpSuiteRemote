// HTTP 历史记录的身份类型。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识 Burp 的一条 HTTP 历史记录，取值来自 Burp 本身，插件不重新推导。
 *
 * @property value 文本取值。
 */
@JvmInline
@Serializable
value class HistoryIdentifier(val value: String)
