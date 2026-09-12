package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState
import java.time.Instant

/**
 * `intercept` 的一行：拦截队列的读模型。
 *
 * plan §18 还列了 `intercept_message`，但拦截报文本体目前没有端口在取，因此先不建。
 */
@Entity(tableName = "intercept")
data class InterceptRecordEntity(
    /** 拦截项身份，主键。 */
    @PrimaryKey
    @ColumnInfo(name = "intercept_identifier")
    val interceptIdentifier: String,
    /** 产生这条记录的事件序号；未知时为 null。 */
    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Long?,
    /** 拦截项产生的时刻，毫秒整数。 */
    @ColumnInfo(name = "created_at")
    val createdAtMilliseconds: Long,
    /** 最后一次变更的时刻，毫秒整数。 */
    @ColumnInfo(name = "updated_at")
    val updatedAtMilliseconds: Long,
    /** 队列里的归宿，按枚举名存放。 */
    @ColumnInfo(name = "state")
    val state: String,
    /** 目标主机。 */
    @ColumnInfo(name = "host")
    val host: String?,
    /** HTTP 方法。 */
    @ColumnInfo(name = "method")
    val method: String?,
    /** 请求路径。 */
    @ColumnInfo(name = "path")
    val path: String?,
)

/** 实体还原成领域读模型。 */
internal fun InterceptRecordEntity.toDomain(): InterceptRecord =
    InterceptRecord(
        interceptIdentifier = InterceptIdentifier(interceptIdentifier),
        sequenceNumber = sequenceNumber,
        createdAt = Instant.ofEpochMilli(createdAtMilliseconds),
        updatedAt = Instant.ofEpochMilli(updatedAtMilliseconds),
        state = interceptStateFromStorageValue(state),
        host = host,
        method = method,
        path = path,
    )

/** 领域读模型落成实体。 */
internal fun InterceptRecord.toEntity(): InterceptRecordEntity =
    InterceptRecordEntity(
        interceptIdentifier = interceptIdentifier.value,
        sequenceNumber = sequenceNumber,
        createdAtMilliseconds = createdAt.toEpochMilli(),
        updatedAtMilliseconds = updatedAt.toEpochMilli(),
        state = state.name,
        host = host,
        method = method,
        path = path,
    )

// 理由同 HistoryRecordEntity：认不出的取值落回队列最前端的状态，不让一列坏数据毁掉整个列表。
private fun interceptStateFromStorageValue(storageValue: String): InterceptState =
    InterceptState.entries.firstOrNull { interceptState -> interceptState.name == storageValue }
        ?: InterceptState.Pending
