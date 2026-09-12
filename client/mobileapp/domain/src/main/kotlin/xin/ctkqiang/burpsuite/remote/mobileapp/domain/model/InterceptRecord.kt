package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import java.time.Instant

/**
 * 界面与领域读的一条拦截项。
 *
 * 未取到的列一律为 null，理由同 [HistoryRecord]：不知道与默认值必须能区分开。
 */
data class InterceptRecord(
    /** 拦截项身份，由 Burp 分配。 */
    val interceptIdentifier: InterceptIdentifier,
    /** 产生这条记录的事件序号。 */
    val sequenceNumber: Long?,
    /** 拦截项产生的时刻。 */
    val createdAt: Instant,
    /** 最后一次变更的时刻。 */
    val updatedAt: Instant,
    /** 队列里的归宿。 */
    val state: InterceptState,
    /** 目标主机。 */
    val host: String?,
    /** HTTP 方法。 */
    val method: String?,
    /** 请求路径。 */
    val path: String?,
)
