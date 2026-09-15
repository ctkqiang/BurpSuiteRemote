// Repeater 请求的读模型。

package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import java.time.Instant

/**
 * 界面与领域读的一条 Repeater 请求。
 *
 * 未取到的列一律为 null，理由同 [InterceptRecord]：不知道与默认值必须能区分开。
 * `isExecuting` 是从状态机派生的布尔，不是独立字段——它由三个事件序列驱动。
 */
data class RepeaterRecord(
    /** Repeater 请求身份，插件侧分配。 */
    val repeaterRequestIdentifier: RepeaterRequestIdentifier,
    /** 产生这条记录的事件序号。 */
    val sequenceNumber: Long?,
    /** 请求创建时刻。 */
    val createdAt: Instant,
    /** 最后一次变更的时刻。 */
    val updatedAt: Instant,
    /** 原始请求文本（HTTP 请求头+体）。 */
    val requestText: String?,
    /** Tab 标签名；插件侧没给就为 null。 */
    val tabName: String?,
    /** 最后一次执行的时刻；还没执行过为 null。 */
    val lastExecutedAt: Instant?,
    /** 最后一次执行的 HTTP 状态码；还没执行过为 null。 */
    val lastStatusCode: Int?,
    /** 最后一次执行的耗时（毫秒）；还没执行过为 null。 */
    val lastDurationMilliseconds: Long?,
    /** 最后一次执行是否失败；null 表示还没执行过。 */
    val lastExecutionFailed: Boolean?,
    /** 当前是否正在执行中。 */
    val isExecuting: Boolean,
)
