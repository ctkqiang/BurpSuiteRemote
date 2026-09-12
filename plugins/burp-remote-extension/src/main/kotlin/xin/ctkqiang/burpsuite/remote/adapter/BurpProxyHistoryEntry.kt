// Burp 代理历史条目在适配层内的最小视图。

package xin.ctkqiang.burpsuite.remote.adapter

/**
 * 一条 Burp 代理历史条目在适配层内的最小视图，报文正文一律按需读取。
 *
 * 映射逻辑只认识这几个字段，于是它可以脱离 Montoya 与运行中的 Burp 单独测试（rules.md §13）。
 */
interface BurpProxyHistoryEntry {
    /** Burp 记录该条目的时刻，毫秒。 */
    val occurredAtEpochMilliseconds: Long

    /** 请求方法。 */
    val method: String

    /** 目标主机名，不含端口。 */
    val host: String

    /** 目标端口。 */
    val port: Int

    /** 是否经 TLS。 */
    val isSecure: Boolean

    /** 请求路径，含查询串。 */
    val path: String

    /** 承接该请求的代理监听端口。 */
    val listenerPort: Int

    /** 响应状态码；尚无响应时为 0。 */
    val statusCode: Int

    /** Burp 判定的响应 MIME 类型描述。 */
    val mimeTypeText: String

    /** 是否已经拿到响应。 */
    val hasResponse: Boolean

    /** 读取请求头原文，含请求行。 */
    fun readRequestHeadersText(): String

    /** 读取请求体原文。 */
    fun readRequestBodyText(): String

    /** 读取响应头原文，含状态行；尚无响应时为 null。 */
    fun readResponseHeadersText(): String?

    /** 读取响应体原文；尚无响应时为 null。 */
    fun readResponseBodyText(): String?
}
