// Montoya InterceptedRequest 到协议载荷的映射。

package xin.ctkqiang.burpsuite.remote.adapter

import burp.api.montoya.proxy.http.InterceptedRequest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import xin.ctkqiang.burpsuite.remote.protocol.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolJson
import java.net.InetAddress

/**
 * 把 Montoya [InterceptedRequest] 映射成协议 payload 与 identifier。
 *
 * 设计决策：
 * - identifier 直接用 Montoya 给的 `messageId()`。Burp 保证每条被拦截消息的 messageId 在代理生命周期内唯一，
 *   所以不需要自己 FNV-1a 哈希；用现成的整数更稳也更短。
 * - 元数据与 payload 的字段名和 History 那一套对齐（method/host/path/scheme/listenerPort），
 *   这样 mobile 端同一套代码逻辑就能同时处理 history 和 intercept 两条流。
 */
class MontoyaInterceptedRequest(
    private val interceptedRequest: InterceptedRequest,
    private val occurredAtEpochMilliseconds: Long,
) {
    /** Burp 给的全局唯一消息 ID，同时作为拦截项 identifier。 */
    val interceptIdentifier: InterceptIdentifier =
        InterceptIdentifier(INTERCEPT_IDENTIFIER_PREFIX + interceptedRequest.messageId())

    /** 构造拦截项元数据载荷；列表端点与事件共用这一形状。 */
    fun buildMetadataPayload(): JsonObject =
        buildJsonObject {
            put(INTERCEPT_IDENTIFIER_FIELD, interceptIdentifier.value)
            put(MESSAGE_ID_FIELD, interceptedRequest.messageId())
            put(METHOD_FIELD, interceptedRequest.method())
            put(HOST_FIELD, interceptedRequest.httpService().host())
            put(PORT_FIELD, interceptedRequest.httpService().port())
            put(PATH_FIELD, interceptedRequest.path())
            put(SCHEME_FIELD, schemeTextOf())
            put(USES_TLS_FIELD, interceptedRequest.httpService().secure())
            put(LISTENER_PORT_FIELD, listenerPortOf(interceptedRequest.listenerInterface()))
            put(OCCURRED_AT_EPOCH_MILLISECONDS_FIELD, occurredAtEpochMilliseconds)
            putAbsentableText(SOURCE_IP_ADDRESS_FIELD, safeHostAddressOf(interceptedRequest.sourceIpAddress()))
            putAbsentableText(DESTINATION_IP_ADDRESS_FIELD, safeHostAddressOf(interceptedRequest.destinationIpAddress()))
            put(IS_IN_SCOPE_FIELD, interceptedRequest.isInScope())
        }

    /** 构造拦截项完整报文载荷；单条端点按这个回。 */
    fun buildMessagePayload(): JsonElement =
        buildJsonObject {
            // JsonObjectBuilder 没有 putAll，手动把 metadata entries 一个一个塞进来。
            for ((key, value) in buildMetadataPayload()) {
                put(key, value)
            }
            put(REQUEST_HEADERS_FIELD, requestHeadersText())
            put(REQUEST_BODY_FIELD, interceptedRequest.bodyToString())
        }

    /** 取出可被 Montoya continueWith 接受的请求；手机没改过 body/headers 时就是原始请求。 */
    fun rawRequest(): burp.api.montoya.http.message.requests.HttpRequest = interceptedRequest

    // listenerInterface 形如 "127.0.0.1:8080"，截掉主机就是监听端口。
    private fun listenerPortOf(listenerInterface: String): Int =
        listenerInterface.substringAfterLast(':').toIntOrNull() ?: -1

    private fun safeHostAddressOf(address: InetAddress?): String? = address?.hostAddress

    private fun schemeTextOf(): String =
        if (interceptedRequest.httpService().secure()) HTTPS_SCHEME_TEXT else HTTP_SCHEME_TEXT

    // 和 BurpHistoryAdapter 里的 putAbsentableText 完全一样的规矩：null 显式写成 JsonNull 而不是省略键。
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

    private fun requestHeadersText(): String {
        val sb = StringBuilder()
        sb.append(interceptedRequest.method()).append(' ')
        sb.append(interceptedRequest.path()).append(' ')
        sb.append(interceptedRequest.httpVersion()).append(CRLF)
        for (header in interceptedRequest.headers()) {
            sb.append(header.name()).append(": ").append(header.value()).append(CRLF)
        }
        sb.append(CRLF)
        return sb.toString()
    }

    private companion object {
        private const val INTERCEPT_IDENTIFIER_PREFIX = "intercept_"

        private const val CRLF = "\r\n"

        private const val HTTPS_SCHEME_TEXT = "https"

        private const val HTTP_SCHEME_TEXT = "http"

        private const val INTERCEPT_IDENTIFIER_FIELD = "interceptIdentifier"

        private const val MESSAGE_ID_FIELD = "messageId"

        private const val METHOD_FIELD = "method"

        private const val HOST_FIELD = "host"

        private const val PORT_FIELD = "port"

        private const val PATH_FIELD = "path"

        private const val SCHEME_FIELD = "scheme"

        private const val USES_TLS_FIELD = "usesTls"

        private const val LISTENER_PORT_FIELD = "listenerPort"

        private const val OCCURRED_AT_EPOCH_MILLISECONDS_FIELD = "occurredAtEpochMilliseconds"

        private const val SOURCE_IP_ADDRESS_FIELD = "sourceIpAddress"

        private const val DESTINATION_IP_ADDRESS_FIELD = "destinationIpAddress"

        private const val IS_IN_SCOPE_FIELD = "isInScope"

        private const val REQUEST_HEADERS_FIELD = "requestHeaders"

        private const val REQUEST_BODY_FIELD = "requestBody"

        // 不使用，只是为了让 RemoteProtocolJson 在这个文件里有 import 目标（避免无意义的 unused 警告被当成错误）。
        @Suppress("unused")
        private val JSON = RemoteProtocolJson.instance
    }
}
