// Remote Repeater 内存存储：插件端自己维护手机推送过来的请求，支持远程执行。

package xin.ctkqiang.burpsuite.remote.adapter

import burp.api.montoya.http.message.HttpRequestResponse
import xin.ctkqiang.burpsuite.remote.protocol.RepeaterRequestIdentifier
import java.time.Instant

/**
 * 一条存放在 Remote Repeater store 里的请求。
 *
 * 这不是 Burp PC Repeater tab 里的条目（Montoya API 不给读），而是手机端通过 REST 推过来、
 * 由插件代为存放在内存里的"Remote Repeater"。手机要执行时，插件从这里取出请求、
 * 用 `montoyaApi.http().sendRequest()` 真发出去。
 */
data class StoredRepeaterRequest(
    /** 唯一标识；格式 `repeater_<epochMillis>`，由 [BurpRepeaterStore.create] 分配。 */
    val identifier: RepeaterRequestIdentifier,
    /** 完整 HTTP 请求文本（请求行 + 头部 + 空行 + 可选 body）；供 `HttpRequest.httpRequest()` 重新解析。 */
    val requestText: String,
    /** 可选 tab 名；用于同时 sendToRepeater 到 Burp PC 的 Repeater tab。 */
    val tabName: String?,
    /** 创建时刻（epoch millis）。 */
    val createdAtEpochMilliseconds: Long,
    /** 最近一次执行的结束时刻；null 表示从未执行。 */
    val lastExecutedAtEpochMilliseconds: Long? = null,
    /** 最近一次执行产生的 statusCode；null 表示从未执行或执行失败。 */
    val lastStatusCode: Int? = null,
    /** 最近一次执行产生的响应头部文本；null 表示从未执行。 */
    val lastResponseHeaders: String? = null,
    /** 最近一次执行产生的响应 body 文本；null 表示从未执行。 */
    val lastResponseBody: String? = null,
    /** 最近一次执行耗时（millis）。 */
    val lastDurationMilliseconds: Long? = null,
)

/**
 * Remote Repeater 内存存储。
 *
 * 只有一台 Burp、内存大小可忽略（手机 Repeater 条目不会上百），所以一把锁 + LinkedHashMap 就够了。
 * 插件卸载时会一起 clear；进程死了就没了——和 Burp PC Repeater tab 里的条目同等地位（都不落盘）。
 */
class BurpRepeaterStore {
    private val lock = Any()
    private val requests = LinkedHashMap<String, StoredRepeaterRequest>()

    /** 创建一条新的 repeater 请求；返回分配好的 identifier（当前时间戳当 ID，足够唯一）。 */
    fun create(
        requestText: String,
        tabName: String?,
        now: Instant,
    ): StoredRepeaterRequest {
        val identifier = RepeaterRequestIdentifier(REPEATER_IDENTIFIER_PREFIX + now.toEpochMilli())
        val stored =
            StoredRepeaterRequest(
                identifier = identifier,
                requestText = requestText,
                tabName = tabName,
                createdAtEpochMilliseconds = now.toEpochMilli(),
            )
        synchronized(lock) {
            requests[identifier.value] = stored
        }
        return stored
    }

    /** 取单条；REST GET /v1/repeaters/{id} 和 execute endpoint 都会用。 */
    fun find(identifier: RepeaterRequestIdentifier): StoredRepeaterRequest? =
        synchronized(lock) { requests[identifier.value] }

    /** 所有条目（按创建顺序）；REST GET /v1/repeaters 用。 */
    fun snapshot(): List<StoredRepeaterRequest> = synchronized(lock) { requests.values.toList() }

    /** 更新最近一次执行结果；execute 完成后由调用方传入实际 response。 */
    fun updateWithResult(
        identifier: RepeaterRequestIdentifier,
        response: HttpRequestResponse?,
        durationMilliseconds: Long,
        now: Instant,
    ): StoredRepeaterRequest? {
        synchronized(lock) {
            val existing = requests[identifier.value] ?: return null
            val updated =
                existing.copy(
                    lastExecutedAtEpochMilliseconds = now.toEpochMilli(),
                    lastStatusCode = response?.response()?.statusCode()?.toInt(),
                    lastResponseHeaders = response?.response()?.let(::headersTextOf),
                    lastResponseBody = response?.response()?.bodyToString(),
                    lastDurationMilliseconds = durationMilliseconds,
                )
            requests[identifier.value] = updated
            return updated
        }
    }

    /** 插件卸载时清空。 */
    fun clear() {
        synchronized(lock) { requests.clear() }
    }

    private fun headersTextOf(response: burp.api.montoya.http.message.responses.HttpResponse): String {
        val bytes = response.toByteArray().getBytes()
        val bodyOffset = response.bodyOffset().coerceIn(0, bytes.size)
        return String(bytes, 0, bodyOffset, Charsets.UTF_8)
    }

    private companion object {
        private const val REPEATER_IDENTIFIER_PREFIX = "repeater_"
    }
}
