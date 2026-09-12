package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionCheckpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName
import java.time.Instant

/**
 * `projection_checkpoint` 的一行。
 *
 * plan §38 把这一列写作 `last_sequence`，这里补全成 `last_sequence_number`，与事件表的
 * `sequence_number` 保持同一口径（rules.md §3.2 要求写全词）。
 */
@Entity(tableName = "projection_checkpoint")
data class ProjectionCheckpointEntity(
    /** 投影名字，主键。 */
    @PrimaryKey
    @ColumnInfo(name = "projection_name")
    val projectionName: String,
    /** 已经折入的最后序号。 */
    @ColumnInfo(name = "last_sequence_number")
    val lastSequenceNumber: Long,
    /** 最后一次推进的时刻，毫秒整数。 */
    @ColumnInfo(name = "updated_at")
    val updatedAtMilliseconds: Long,
)

/** 实体还原成领域校验点。 */
internal fun ProjectionCheckpointEntity.toProjectionCheckpoint(): ProjectionCheckpoint =
    ProjectionCheckpoint(
        projectionName = ProjectionName(projectionName),
        lastSequenceNumber = lastSequenceNumber,
        updatedAt = Instant.ofEpochMilli(updatedAtMilliseconds),
    )

/** 领域校验点落成实体。 */
internal fun ProjectionCheckpoint.toEntity(): ProjectionCheckpointEntity =
    ProjectionCheckpointEntity(
        projectionName = projectionName.value,
        lastSequenceNumber = lastSequenceNumber,
        updatedAtMilliseconds = updatedAt.toEpochMilli(),
    )
