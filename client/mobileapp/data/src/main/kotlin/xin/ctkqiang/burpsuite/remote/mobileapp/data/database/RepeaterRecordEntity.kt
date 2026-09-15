// Repeater 表的一行：Repeater 请求的读模型。

package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.RepeaterRecord
import java.time.Instant

/**
 * `repeater` 的一行：Repeater 请求的读模型。
 *
 * 大请求体留在插件端；这里只存执行结果与状态变更。
 */
@Entity(tableName = "repeater")
data class RepeaterRecordEntity(
    /** Repeater 请求身份，主键。 */
    @PrimaryKey
    @ColumnInfo(name = "repeater_request_identifier")
    val repeaterRequestIdentifier: String,
    /** 产生这条记录的事件序号；未知时为 null。 */
    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Long?,
    /** 请求创建时刻，毫秒整数。 */
    @ColumnInfo(name = "created_at")
    val createdAtMilliseconds: Long,
    /** 最后一次变更的时刻，毫秒整数。 */
    @ColumnInfo(name = "updated_at")
    val updatedAtMilliseconds: Long,
    /** 原始请求文本（HTTP 请求头+体）；大报文截断或置 null。 */
    @ColumnInfo(name = "request_text")
    val requestText: String?,
    /** Tab 标签名；插件侧没给就为 null。 */
    @ColumnInfo(name = "tab_name")
    val tabName: String?,
    /** 最后一次执行的时刻；还没执行过为 null。 */
    @ColumnInfo(name = "last_executed_at")
    val lastExecutedAtMilliseconds: Long?,
    /** 最后一次执行的 HTTP 状态码；还没执行过为 null。 */
    @ColumnInfo(name = "last_status_code")
    val lastStatusCode: Int?,
    /** 最后一次执行的耗时（毫秒）；还没执行过为 null。 */
    @ColumnInfo(name = "last_duration_milliseconds")
    val lastDurationMilliseconds: Long?,
    /** 最后一次执行是否失败；null 表示还没执行过。 */
    @ColumnInfo(name = "last_execution_failed")
    val lastExecutionFailed: Boolean?,
    /** 当前是否正在执行中。 */
    @ColumnInfo(name = "is_executing")
    val isExecuting: Boolean,
)

/** 实体还原成领域读模型。 */
internal fun RepeaterRecordEntity.toDomain(): RepeaterRecord =
    RepeaterRecord(
        repeaterRequestIdentifier = RepeaterRequestIdentifier(repeaterRequestIdentifier),
        sequenceNumber = sequenceNumber,
        createdAt = Instant.ofEpochMilli(createdAtMilliseconds),
        updatedAt = Instant.ofEpochMilli(updatedAtMilliseconds),
        requestText = requestText,
        tabName = tabName,
        lastExecutedAt = lastExecutedAtMilliseconds?.let { Instant.ofEpochMilli(it) },
        lastStatusCode = lastStatusCode,
        lastDurationMilliseconds = lastDurationMilliseconds,
        lastExecutionFailed = lastExecutionFailed,
        isExecuting = isExecuting,
    )

/** 领域读模型落成实体。 */
internal fun RepeaterRecord.toEntity(): RepeaterRecordEntity =
    RepeaterRecordEntity(
        repeaterRequestIdentifier = repeaterRequestIdentifier.value,
        sequenceNumber = sequenceNumber,
        createdAtMilliseconds = createdAt.toEpochMilli(),
        updatedAtMilliseconds = updatedAt.toEpochMilli(),
        requestText = requestText,
        tabName = tabName,
        lastExecutedAtMilliseconds = lastExecutedAt?.toEpochMilli(),
        lastStatusCode = lastStatusCode,
        lastDurationMilliseconds = lastDurationMilliseconds,
        lastExecutionFailed = lastExecutionFailed,
        isExecuting = isExecuting,
    )
