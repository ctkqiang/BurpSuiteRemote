// HTTP 历史记录的身份类型。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 标识 Burp 的一条 HTTP 历史记录，取值来自 Burp，客户端不重新推导。
 *
 * @property value 文本取值。
 */
@JvmInline
value class HistoryIdentifier(val value: String)
