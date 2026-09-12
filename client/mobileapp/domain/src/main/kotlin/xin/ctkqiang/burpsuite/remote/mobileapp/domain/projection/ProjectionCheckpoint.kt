package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import java.time.Instant

/**
 * 一个投影的进度。
 *
 * 它是「已经折到哪一号」的记账，不是事实本身：投影与日志不一致时以日志为准，照它重建（rules.md §5.6）。
 */
data class ProjectionCheckpoint(
    /** 投影名字，主键。 */
    val projectionName: ProjectionName,
    /** 已经折入的最后序号。 */
    val lastSequenceNumber: Long,
    /** 最后一次推进的时刻。 */
    val updatedAt: Instant,
)
