package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import java.time.Instant

/**
 * 界面与领域读的一条 HTTP 历史记录。
 *
 * 只带元数据：报文本体按标识另行取回，列表因此不必为每一行加载大报文（plan §19、plan §20）。
 * 未取到的列一律为 null，而不是填默认值——「不知道」和「是 0」不是同一件事。
 */
data class HistoryRecord(
    /** 历史记录身份，由 Burp 分配。 */
    val historyIdentifier: HistoryIdentifier,
    /** 产生这条记录的事件序号。 */
    val sequenceNumber: Long?,
    /** 事件发生时刻。 */
    val occurredAt: Instant,
    /** 目标主机。 */
    val host: String?,
    /** HTTP 方法。 */
    val method: String?,
    /** 协议方案，例如 http、https。 */
    val scheme: String?,
    /** 请求路径。 */
    val path: String?,
    /** 响应状态码。 */
    val statusCode: Int?,
    /** 响应 MIME 类型。 */
    val mimeType: String?,
    /** 响应字节数。 */
    val responseLength: Long?,
    /** 是否经 TLS 传输。 */
    val usesTls: Boolean?,
    /** 目标地址。 */
    val destinationInternetProtocolAddress: String?,
    /** 监听端口。 */
    val listenerPort: Int?,
    /** 请求耗时。 */
    val durationMilliseconds: Long?,
    /** 用户是否改写过这条记录。 */
    val isEdited: Boolean,
    /** 用户给这条记录起的标题。 */
    val title: String?,
    /** 这条记录是实时投影还是已归档副本。 */
    val archiveState: HistoryArchiveState,
    /** 已收到的注解条数。 */
    val annotationCount: Int,
    /** 最后一条注解的时刻。 */
    val lastAnnotatedAt: Instant?,
    /** 归档时刻；未归档时为 null。 */
    val savedAt: Instant?,
)
