package xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore

import xin.ctkqiang.burpsuite.remote.mobileapp.data.serialization.JsonObjectAttributes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.EventPayloadAttributes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import java.nio.charset.StandardCharsets

/**
 * 日志事件 → 投影输入。
 *
 * 载荷 JSON 在这里摊平成标量字段，投影层因此只认键值对、不认序列化格式（rules.md §6.1）。
 * 同一事件反复映射得到同样的结果，投影的幂等才有依据。
 */
object JournalEventRecordedEventMapper {
    /** 映射一条事件。 */
    fun map(event: JournalEvent): RecordedEvent =
        RecordedEvent(
            eventIdentifier = event.eventIdentifier,
            sequenceNumber = event.sequenceNumber,
            occurredAt = event.occurredAt,
            eventType = event.eventType,
            aggregateIdentifier = event.aggregateIdentifier,
            payloadAttributes =
                EventPayloadAttributes(
                    JsonObjectAttributes.parse(String(event.payload, StandardCharsets.UTF_8)),
                ),
        )
}
