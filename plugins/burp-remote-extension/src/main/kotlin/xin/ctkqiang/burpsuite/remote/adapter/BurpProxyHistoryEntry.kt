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

    /** 目标主机的互联网协议地址；Burp 没给出时为 null。 */
    val destinationInternetProtocolAddress: String?

    /** 请求路径，含查询串。 */
    val path: String

    /** 承接该请求的代理监听端口。 */
    val listenerPort: Int

    /** 响应状态码；尚无响应时为 null，与 [responseLength]、[durationMilliseconds] 同一条规矩。 */
    val statusCode: Int?

    /** Burp 判定的响应 MIME 类型描述。 */
    val mimeTypeText: String

    /** 响应正文字节数；尚无响应时为 null。 */
    val responseLength: Long?

    /** 请求发出到响应收完的耗时，毫秒；尚无响应时为 null。 */
    val durationMilliseconds: Long?

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

    /** 读取请求原文的原始字节，含请求行、头部与正文；重放按字节走，字符编码不参与。 */
    fun readRequestBytes(): ByteArray
}
