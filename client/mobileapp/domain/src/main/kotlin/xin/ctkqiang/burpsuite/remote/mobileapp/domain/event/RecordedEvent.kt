package xin.ctkqiang.burpsuite.remote.mobileapp.domain.event

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import java.time.Instant

/**
 * 喂给投影的一条事件。
 *
 * 与 [JournalEvent] 的分工：日志要原样保存字节，投影只需要判定用得上的字段，
 * 于是摊平载荷这件事只发生在投影入口这一处。
 */
data class RecordedEvent(
    /** 事件身份；与序号一起组成去重键。 */
    val eventIdentifier: EventIdentifier,
    /** 权威侧分配的单调序号。 */
    val sequenceNumber: Long,
    /** 事件发生时刻。 */
    val occurredAt: Instant,
    /** 事件的线上类型串。 */
    val eventType: EventType,
    /** 事件归属的聚合实例。 */
    val aggregateIdentifier: AggregateIdentifier,
    /** 载荷里的标量字段。 */
    val payloadAttributes: EventPayloadAttributes,
)
