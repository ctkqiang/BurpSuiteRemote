package xin.ctkqiang.burpsuite.remote.mobileapp.domain.event

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import java.time.Instant

/**
 * 事件日志里的一条事实。
 *
 * 载荷保持原始字节：日志是重放的唯一依据，投影重建完全靠它，所以不在这里提前降级成标量字段。
 */
data class JournalEvent(
    /** 事件身份，日志内唯一；与序号一起组成去重键。 */
    val eventIdentifier: EventIdentifier,
    /** 权威侧分配的单调序号；日志内唯一。 */
    val sequenceNumber: Long,
    /** 事件归属的聚合类型。 */
    val aggregateType: AggregateTypeName,
    /** 事件归属的聚合实例。 */
    val aggregateIdentifier: AggregateIdentifier,
    /** 事件的线上类型串。 */
    val eventType: EventType,
    /** 载荷结构的版本，用来在结构变化后仍能重放旧事件。 */
    val eventVersion: Int,
    /** 事件发生时刻。 */
    val occurredAt: Instant,
    /** 载荷原始字节。 */
    val payload: ByteArray,
    /** 事件附带的可扩展元数据。 */
    val metadata: Map<String, String>,
) {
    // 字节数组的相等性是引用比较，data class 自动生成的 equals 在这里必然出错，因此显式按内容比。
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JournalEvent) return false
        return eventIdentifier == other.eventIdentifier &&
            sequenceNumber == other.sequenceNumber &&
            aggregateType == other.aggregateType &&
            aggregateIdentifier == other.aggregateIdentifier &&
            eventType == other.eventType &&
            eventVersion == other.eventVersion &&
            occurredAt == other.occurredAt &&
            payload.contentEquals(other.payload) &&
            metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = eventIdentifier.hashCode()
        result = HASH_MULTIPLIER * result + sequenceNumber.hashCode()
        result = HASH_MULTIPLIER * result + aggregateType.hashCode()
        result = HASH_MULTIPLIER * result + aggregateIdentifier.hashCode()
        result = HASH_MULTIPLIER * result + eventType.hashCode()
        result = HASH_MULTIPLIER * result + eventVersion
        result = HASH_MULTIPLIER * result + occurredAt.hashCode()
        result = HASH_MULTIPLIER * result + payload.contentHashCode()
        result = HASH_MULTIPLIER * result + metadata.hashCode()
        return result
    }

    private companion object {
        private const val HASH_MULTIPLIER = 31
    }
}
