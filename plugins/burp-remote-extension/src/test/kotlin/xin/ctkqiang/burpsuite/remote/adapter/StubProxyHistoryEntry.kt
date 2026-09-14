// 测试替身：一条可控的 Burp 代理历史条目。

package xin.ctkqiang.burpsuite.remote.adapter

import java.nio.charset.StandardCharsets

/** 一条测试用的历史条目；它只实现薄接口，因此映射测试不必构造任何 Montoya 类型。 */
class StubProxyHistoryEntry(
    override val occurredAtEpochMilliseconds: Long = DEFAULT_OCCURRED_AT_EPOCH_MILLISECONDS,
    override val method: String = DEFAULT_METHOD,
    override val host: String = DEFAULT_HOST,
    override val port: Int = DEFAULT_PORT,
    override val isSecure: Boolean = DEFAULT_IS_SECURE,
    override val destinationInternetProtocolAddress: String? = DEFAULT_DESTINATION_INTERNET_PROTOCOL_ADDRESS,
    override val path: String = DEFAULT_PATH,
    override val listenerPort: Int = DEFAULT_LISTENER_PORT,
    override val statusCode: Int? = DEFAULT_STATUS_CODE,
    override val mimeTypeText: String = DEFAULT_MIME_TYPE_TEXT,
    override val responseLength: Long? = DEFAULT_RESPONSE_LENGTH,
    override val durationMilliseconds: Long? = DEFAULT_DURATION_MILLISECONDS,
    override val hasResponse: Boolean = DEFAULT_HAS_RESPONSE,
    private val requestHeadersText: String = DEFAULT_REQUEST_HEADERS_TEXT,
    private val requestBodyText: String = DEFAULT_REQUEST_BODY_TEXT,
    private val responseHeadersText: String? = DEFAULT_RESPONSE_HEADERS_TEXT,
    private val responseBodyText: String? = DEFAULT_RESPONSE_BODY_TEXT,
) : BurpProxyHistoryEntry {
    override fun readRequestHeadersText(): String = requestHeadersText

    override fun readRequestBodyText(): String = requestBodyText

    override fun readResponseHeadersText(): String? = responseHeadersText

    override fun readResponseBodyText(): String? = responseBodyText

    // 由两个文本字段推出来，替身因此自洽：配什么头、什么正文，拿到的字节就是它们的原文。
    override fun readRequestBytes(): ByteArray =
        (readRequestHeadersText() + readRequestBodyText()).toByteArray(StandardCharsets.UTF_8)

    companion object {
        const val DEFAULT_OCCURRED_AT_EPOCH_MILLISECONDS: Long = 1_757_660_000_000L

        const val DEFAULT_METHOD: String = "GET"

        const val DEFAULT_HOST: String = "api.example.com"

        const val DEFAULT_PORT: Int = 443

        const val DEFAULT_IS_SECURE: Boolean = true

        const val DEFAULT_DESTINATION_INTERNET_PROTOCOL_ADDRESS: String = "203.0.113.10"

        const val DEFAULT_PATH: String = "/api/user"

        const val DEFAULT_LISTENER_PORT: Int = 8_080

        const val DEFAULT_STATUS_CODE: Int = 200

        const val DEFAULT_MIME_TYPE_TEXT: String = "JSON"

        const val DEFAULT_RESPONSE_LENGTH: Long = 128L

        const val DEFAULT_DURATION_MILLISECONDS: Long = 42L

        const val DEFAULT_HAS_RESPONSE: Boolean = true

        const val DEFAULT_REQUEST_HEADERS_TEXT: String = "GET /api/user HTTP/1.1\r\nHost: api.example.com\r\n"

        const val DEFAULT_REQUEST_BODY_TEXT: String = "{\"identifier\":\"42\"}"

        const val DEFAULT_RESPONSE_HEADERS_TEXT: String = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n"

        const val DEFAULT_RESPONSE_BODY_TEXT: String = "{\"name\":\"secret\"}"
    }
}
