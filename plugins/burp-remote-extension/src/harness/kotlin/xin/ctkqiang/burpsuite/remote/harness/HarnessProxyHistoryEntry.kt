// 夹具用的代理历史条目：字段与真实 Burp 条目同形，正文固定。

package xin.ctkqiang.burpsuite.remote.harness

import xin.ctkqiang.burpsuite.remote.adapter.BurpProxyHistoryEntry

/** 夹具使用的代理历史条目；正文固定成一小段无害文本，端到端要验证的是事件通道而不是正文读取。 */
class HarnessProxyHistoryEntry(
    override val occurredAtEpochMilliseconds: Long,
    override val method: String,
    override val host: String,
    override val port: Int,
    override val isSecure: Boolean,
    override val path: String,
    override val listenerPort: Int,
    override val statusCode: Int,
    override val mimeTypeText: String,
) : BurpProxyHistoryEntry {
    override val hasResponse: Boolean = true

    override fun readRequestHeadersText(): String = "$method $path HTTP/1.1\r\nHost: $host\r\n"

    override fun readRequestBodyText(): String = ""

    override fun readResponseHeadersText(): String = "HTTP/1.1 $statusCode OK\r\n"

    override fun readResponseBodyText(): String = ""
}
