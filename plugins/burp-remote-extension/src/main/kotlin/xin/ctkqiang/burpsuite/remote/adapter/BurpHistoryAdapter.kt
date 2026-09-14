// Burp 代理历史到协议载荷的映射。

package xin.ctkqiang.burpsuite.remote.adapter

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import xin.ctkqiang.burpsuite.remote.protocol.HistoryIdentifier
import java.nio.charset.StandardCharsets

/**
 * Burp 代理历史到协议载荷的映射（plan §53）。
 *
 * 出口只有协议类型的字符串与 JSON，Montoya 的类型在 [MontoyaProxyHistorySource] 就已拆掉，不会流到线上（rules.md §11）。
 */
class BurpHistoryAdapter(private val historySource: BurpProxyHistorySource) {
    /** 读出全部历史条目的元数据视图；正文不在这里取。 */
    fun readHistoryEntries(): List<BurpProxyHistoryEntry> = historySource.readHistoryEntries()

    /** 构造 `GET /v1/history` 的载荷：只有元数据，正文留给按标识取回的那条端点。 */
    fun buildHistoryListPayload(): JsonElement =
        buildJsonObject {
            putJsonArray(HISTORY_ITEMS_FIELD) {
                historySource.readHistoryEntries().forEach { entry -> add(buildMetadataPayload(entry)) }
            }
        }

    /** 构造 `GET /v1/history/{historyIdentifier}` 的载荷；标识在当前历史里找不到时返回 null。 */
    fun buildHistoryMessagePayload(historyIdentifier: HistoryIdentifier): JsonElement? =
        historySource.readHistoryEntries()
            .firstOrNull { entry -> toHistoryIdentifier(entry) == historyIdentifier }
            ?.let { entry -> buildMessagePayload(entry) }

    /** 构造单条历史的元数据载荷；事件与列表共用同一形状（plan §82）。 */
    fun buildMetadataPayload(entry: BurpProxyHistoryEntry): JsonObject =
        buildJsonObject {
            put(HISTORY_IDENTIFIER_FIELD, toHistoryIdentifier(entry).value)
            put(METHOD_FIELD, entry.method)
            put(HOST_FIELD, entry.host)
            put(PATH_FIELD, entry.path)
            put(SCHEME_FIELD, schemeTextOf(entry))
            putAbsentableNumber(STATUS_CODE_FIELD, entry.statusCode)
            put(MIME_TYPE_FIELD, entry.mimeTypeText)
            put(USES_TLS_FIELD, entry.isSecure)
            put(LISTENER_PORT_FIELD, entry.listenerPort)
            putAbsentableText(DESTINATION_INTERNET_PROTOCOL_ADDRESS_FIELD, entry.destinationInternetProtocolAddress)
            putAbsentableNumber(RESPONSE_LENGTH_FIELD, entry.responseLength)
            putAbsentableNumber(DURATION_MILLISECONDS_FIELD, entry.durationMilliseconds)
        }

    // 没有值的字段写成显式 null，而不是省略键：键集因此固定，客户端能区分"这次没报"与"这类条目根本没有这个字段"。
    // 两个分支各写各的，是因为 `put` 按值的静态类型重载：`fieldValue ?: JsonNull` 会被推成 Any，反而一个重载都匹配不上。
    private fun JsonObjectBuilder.putAbsentableText(
        fieldName: String,
        fieldValue: String?,
    ) {
        if (fieldValue == null) {
            put(fieldName, JsonNull)
        } else {
            put(fieldName, fieldValue)
        }
    }

    private fun JsonObjectBuilder.putAbsentableNumber(
        fieldName: String,
        fieldValue: Number?,
    ) {
        if (fieldValue == null) {
            put(fieldName, JsonNull)
        } else {
            put(fieldName, fieldValue)
        }
    }

    // 协议方案由是否经 TLS 推出，两处各写一遍迟早会分叉；线上沿用客户端既有的小写写法。
    // 作用域适配器也要拼主机地址，因此它对模块内可见：两边用同一份事实，不会各推一遍。
    internal fun schemeTextOf(entry: BurpProxyHistoryEntry): String =
        if (entry.isSecure) HTTPS_SCHEME_TEXT else HTTP_SCHEME_TEXT

    /**
     * 由 Burp 给出的字段推导条目身份。
     *
     * Montoya 2025.5 的历史条目没有稳定标识，只能把 Burp 报出来的字段拼成规范串再散列；同一批字段必然得到同一个身份，
     * 于是列表、按标识取回与事件三处口径一致。
     */
    fun toHistoryIdentifier(entry: BurpProxyHistoryEntry): HistoryIdentifier =
        HistoryIdentifier(HISTORY_IDENTIFIER_PREFIX + hexTextOf(digestOf(entry)))

    private fun buildMessagePayload(entry: BurpProxyHistoryEntry): JsonObject =
        buildJsonObject {
            put(HISTORY_IDENTIFIER_FIELD, toHistoryIdentifier(entry).value)
            put(METHOD_FIELD, entry.method)
            put(HOST_FIELD, entry.host)
            put(PATH_FIELD, entry.path)
            putAbsentableNumber(STATUS_FIELD, entry.statusCode)
            put(REQUEST_HEADERS_FIELD, entry.readRequestHeadersText())
            put(REQUEST_BODY_FIELD, entry.readRequestBodyText())
            put(RESPONSE_HEADERS_FIELD, entry.readResponseHeadersText())
            put(RESPONSE_BODY_FIELD, entry.readResponseBodyText())
        }

    // 字段之间用 NUL 分隔：HTTP 头与路径里不会出现 NUL，拼接不会跨字段撞车。
    // 状态码缺席时用 [ABSENT_STATUS_CODE_TEXT] 而不是空串：空串会让「没有状态码」与「状态码为空文本」撞成同一个身份。
    private fun digestOf(entry: BurpProxyHistoryEntry): Long {
        val canonicalText =
            listOf(
                entry.listenerPort.toString(),
                entry.isSecure.toString(),
                entry.host,
                entry.port.toString(),
                entry.method,
                entry.path,
                entry.occurredAtEpochMilliseconds.toString(),
                entry.statusCode?.toString() ?: ABSENT_STATUS_CODE_TEXT,
                entry.mimeTypeText,
            ).joinToString(IDENTIFIER_FIELD_SEPARATOR)
        return fnvOneAHashOf(canonicalText.toByteArray(StandardCharsets.UTF_8))
    }

    // FNV-1a 64 位：只用来生成身份文本，不承担安全职责，实现短且跨运行稳定。
    private fun fnvOneAHashOf(bytes: ByteArray): Long {
        var hash = FNV_OFFSET_BASIS
        for (singleByte in bytes) {
            hash = hash xor (singleByte.toLong() and BYTE_VALUE_MASK)
            hash *= FNV_PRIME
        }
        return hash
    }

    private fun hexTextOf(value: Long): String =
        value.toULong().toString(radix = HEXADECIMAL_RADIX).padStart(HISTORY_IDENTIFIER_HEX_LENGTH, HEXADECIMAL_PADDING)

    private companion object {
        private const val HISTORY_IDENTIFIER_PREFIX = "history_"

        private const val HISTORY_IDENTIFIER_HEX_LENGTH = 16

        private const val HEXADECIMAL_RADIX = 16

        private const val HEXADECIMAL_PADDING = '0'

        private const val IDENTIFIER_FIELD_SEPARATOR = "\u0000"

        private const val ABSENT_STATUS_CODE_TEXT = "no-response"

        private const val FNV_OFFSET_BASIS = -3750763034362895579L

        private const val FNV_PRIME = 1099511628211L

        private const val BYTE_VALUE_MASK = 0xFFL

        private const val HISTORY_ITEMS_FIELD = "historyItems"

        private const val HISTORY_IDENTIFIER_FIELD = "historyIdentifier"

        private const val METHOD_FIELD = "method"

        private const val HOST_FIELD = "host"

        private const val PATH_FIELD = "path"

        // 元数据与事件走领域字段名，客户端投射按同一套名字取值。
        private const val SCHEME_FIELD = "scheme"

        private const val STATUS_CODE_FIELD = "statusCode"

        private const val MIME_TYPE_FIELD = "mimeType"

        private const val USES_TLS_FIELD = "usesTls"

        private const val LISTENER_PORT_FIELD = "listenerPort"

        private const val DESTINATION_INTERNET_PROTOCOL_ADDRESS_FIELD = "destinationInternetProtocolAddress"

        private const val RESPONSE_LENGTH_FIELD = "responseLength"

        private const val DURATION_MILLISECONDS_FIELD = "durationMilliseconds"

        private const val HTTPS_SCHEME_TEXT = "https"

        private const val HTTP_SCHEME_TEXT = "http"

        // 按标识取回报文的那条端点沿用 `status`：它对应的客户端 DTO 是既有的协议契约，改名要一并升版本（rules.md §7.6）。
        private const val STATUS_FIELD = "status"

        private const val REQUEST_HEADERS_FIELD = "requestHeaders"

        private const val REQUEST_BODY_FIELD = "requestBody"

        private const val RESPONSE_HEADERS_FIELD = "responseHeaders"

        private const val RESPONSE_BODY_FIELD = "responseBody"
    }
}
