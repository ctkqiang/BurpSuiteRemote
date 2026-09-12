// Burp 代理历史到协议载荷的映射。

package xin.ctkqiang.burpsuite.remote.adapter

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
            put(STATUS_FIELD, entry.statusCode)
        }

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
            put(STATUS_FIELD, entry.statusCode)
            put(REQUEST_HEADERS_FIELD, entry.readRequestHeadersText())
            put(REQUEST_BODY_FIELD, entry.readRequestBodyText())
            put(RESPONSE_HEADERS_FIELD, entry.readResponseHeadersText())
            put(RESPONSE_BODY_FIELD, entry.readResponseBodyText())
        }

    // 字段之间用 NUL 分隔：HTTP 头与路径里不会出现 NUL，拼接不会跨字段撞车。
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
                entry.statusCode.toString(),
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

        private const val FNV_OFFSET_BASIS = -3750763034362895579L

        private const val FNV_PRIME = 1099511628211L

        private const val BYTE_VALUE_MASK = 0xFFL

        private const val HISTORY_ITEMS_FIELD = "historyItems"

        private const val HISTORY_IDENTIFIER_FIELD = "historyIdentifier"

        private const val METHOD_FIELD = "method"

        private const val HOST_FIELD = "host"

        private const val PATH_FIELD = "path"

        private const val STATUS_FIELD = "status"

        private const val REQUEST_HEADERS_FIELD = "requestHeaders"

        private const val REQUEST_BODY_FIELD = "requestBody"

        private const val RESPONSE_HEADERS_FIELD = "responseHeaders"

        private const val RESPONSE_BODY_FIELD = "responseBody"
    }
}
