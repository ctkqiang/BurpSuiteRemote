// Montoya 版代理历史读取实现。

package xin.ctkqiang.burpsuite.remote.adapter

import burp.api.montoya.http.message.HttpMessage
import burp.api.montoya.http.message.MimeType
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse
import burp.api.montoya.proxy.Proxy
import burp.api.montoya.proxy.ProxyHttpRequestResponse
import java.nio.charset.StandardCharsets

/**
 * 用 Montoya 的代理历史实现读取端口，是全仓唯一把 `ProxyHttpRequestResponse` 降级成适配层视图的地方。
 *
 * Montoya 2025.5 的历史条目没有稳定标识，所以这里只搬运字段，身份由 [BurpHistoryAdapter] 按 Burp 给出的字段推导。
 */
class MontoyaProxyHistorySource(private val proxy: Proxy) : BurpProxyHistorySource {
    override fun readHistoryEntries(): List<BurpProxyHistoryEntry> =
        proxy.history().map { historyEntry -> MontoyaProxyHistoryEntry(historyEntry) }
}

// 逐字段取值、不留 ProxyHttpRequestResponse 引用：留一份引用等于把整段报文钉在内存里。
private class MontoyaProxyHistoryEntry(private val historyEntry: ProxyHttpRequestResponse) : BurpProxyHistoryEntry {
    private val request: HttpRequest = historyEntry.finalRequest()

    private val response: HttpResponse? = if (historyEntry.hasResponse()) historyEntry.response() else null

    override val occurredAtEpochMilliseconds: Long = historyEntry.time().toInstant().toEpochMilli()

    override val method: String = request.method()

    override val host: String = request.httpService().host()

    override val port: Int = request.httpService().port()

    override val isSecure: Boolean = request.httpService().secure()

    override val destinationInternetProtocolAddress: String? = request.httpService().ipAddress()

    override val path: String = request.path()

    override val listenerPort: Int = historyEntry.listenerPort()

    override val statusCode: Int = if (response != null) response.statusCode().toInt() else NO_RESPONSE_STATUS_CODE

    override val mimeTypeText: String = describeMimeType(historyEntry.mimeType())

    // 没有响应就没有正文长度可言，这里给 null 而不是 0：0 是个合法的空响应长度，两者不能混为一谈。
    override val responseLength: Long? = response?.let { historyResponse -> historyResponse.body().length().toLong() }

    // 同理，没有响应时计时数据没有意义；Montoya 的 timingData() 直接返回对象，不判空会读到一段零耗时。
    override val durationMilliseconds: Long? =
        response?.let { historyEntry.timingData().timeBetweenRequestSentAndEndOfResponse().toMillis() }

    override val hasResponse: Boolean = response != null

    override fun readRequestHeadersText(): String = headersTextOf(request)

    override fun readRequestBodyText(): String = request.bodyToString()

    override fun readResponseHeadersText(): String? =
        response?.let { historyResponse ->
            headersTextOf(
                historyResponse,
            )
        }

    override fun readResponseBodyText(): String? = response?.bodyToString()

    // bodyOffset 正好是正文起点，切在它上面就能把头部与正文分开，不必自己解析空行。
    private fun headersTextOf(message: HttpMessage): String {
        val messageBytes = message.toByteArray().getBytes()
        val bodyOffset = message.bodyOffset().coerceIn(0, messageBytes.size)
        return String(messageBytes, 0, bodyOffset, StandardCharsets.UTF_8)
    }

    private fun describeMimeType(mimeType: MimeType?): String = mimeType?.description() ?: MIME_TYPE_UNKNOWN_TEXT

    private companion object {
        private const val NO_RESPONSE_STATUS_CODE = 0

        private const val MIME_TYPE_UNKNOWN_TEXT = "unknown"
    }
}
