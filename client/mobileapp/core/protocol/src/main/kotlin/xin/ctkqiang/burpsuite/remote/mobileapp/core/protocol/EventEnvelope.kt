// 线上事件的统一信封。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.AggregateIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EpochMillisecondsInstantSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EventIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EventTypeSerializer
import java.time.Instant

/**
 * 线上事件的统一信封（plan §8）；字段名就是协议契约，改名等于改协议。
 *
 * @property protocolVersion 协议版本，收发双方都要校验。
 * @property eventIdentifier 事件身份，与序号一起组成去重键。
 * @property sequenceNumber 权威侧分配的单调序号，断洞靠它发现。
 * @property occurredAt 事件发生时刻，毫秒整数而不是 ISO 文本。
 * @property eventType 事件的线上类型串。
 * @property aggregateType 事件归属的聚合类型。
 * @property aggregateIdentifier 事件归属的聚合实例。
 * @property payload 载荷，按 eventType 解释。
 */
@Serializable
data class EventEnvelope(
    val protocolVersion: Int,
    @Serializable(with = EventIdentifierSerializer::class)
    val eventIdentifier: EventIdentifier,
    val sequenceNumber: Long,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    val occurredAt: Instant,
    @Serializable(with = EventTypeSerializer::class)
    val eventType: EventType,
    val aggregateType: AggregateType,
    @Serializable(with = AggregateIdentifierSerializer::class)
    val aggregateIdentifier: AggregateIdentifier,
    val payload: JsonObject,
)
