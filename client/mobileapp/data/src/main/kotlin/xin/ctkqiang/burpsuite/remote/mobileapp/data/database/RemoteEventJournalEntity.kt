package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.data.serialization.JsonObjectAttributes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.AggregateTypeName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import java.time.Instant

/**
 * `remote_event_journal` 的一行。
 *
 * 列名与唯一约束照 plan §11：身份与序号各自唯一，去重就压在这两条约束上。
 * 本表没有更新与删除的调用方——事件一旦写入就是事实（rules.md §5.4）。
 */
@Entity(
    tableName = "remote_event_journal",
    indices = [Index(value = ["sequence_number"], unique = true)],
)
data class RemoteEventJournalEntity(
    /** 事件身份，主键。 */
    @PrimaryKey
    @ColumnInfo(name = "event_identifier")
    val eventIdentifier: String,
    /** 权威侧分配的单调序号，唯一。 */
    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Long,
    /** 事件归属的聚合类型。 */
    @ColumnInfo(name = "aggregate_type")
    val aggregateType: String,
    /** 事件归属的聚合实例。 */
    @ColumnInfo(name = "aggregate_identifier")
    val aggregateIdentifier: String,
    /** 事件的线上类型串。 */
    @ColumnInfo(name = "event_type")
    val eventType: String,
    /** 载荷结构的版本。 */
    @ColumnInfo(name = "event_version")
    val eventVersion: Int,
    /** 事件发生时刻，毫秒整数。 */
    @ColumnInfo(name = "timestamp")
    val occurredAtMilliseconds: Long,
    /** 载荷原始字节。 */
    @ColumnInfo(name = "payload")
    val payload: ByteArray,
    /** 元数据的 JSON 对象文本。 */
    @ColumnInfo(name = "metadata")
    val metadataText: String,
    /** 本地收到的时刻，毫秒整数；与事件自身发生的时刻分开记。 */
    @ColumnInfo(name = "received_at")
    val receivedAtMilliseconds: Long,
)

/** 实体还原成领域日志事件。 */
internal fun RemoteEventJournalEntity.toJournalEvent(): JournalEvent =
    JournalEvent(
        eventIdentifier = EventIdentifier(eventIdentifier),
        sequenceNumber = sequenceNumber,
        aggregateType = AggregateTypeName(aggregateType),
        aggregateIdentifier = AggregateIdentifier(aggregateIdentifier),
        eventType = EventType(eventType),
        eventVersion = eventVersion,
        occurredAt = Instant.ofEpochMilli(occurredAtMilliseconds),
        payload = payload,
        metadata = JsonObjectAttributes.parse(metadataText),
    )

/** 领域日志事件落成实体；收到时刻由调用方的时钟给出。 */
internal fun JournalEvent.toEntity(receivedAt: Instant): RemoteEventJournalEntity =
    RemoteEventJournalEntity(
        eventIdentifier = eventIdentifier.value,
        sequenceNumber = sequenceNumber,
        aggregateType = aggregateType.value,
        aggregateIdentifier = aggregateIdentifier.value,
        eventType = eventType.value,
        eventVersion = eventVersion,
        occurredAtMilliseconds = occurredAt.toEpochMilli(),
        payload = payload,
        metadataText = JsonObjectAttributes.encode(metadata),
        receivedAtMilliseconds = receivedAt.toEpochMilli(),
    )
