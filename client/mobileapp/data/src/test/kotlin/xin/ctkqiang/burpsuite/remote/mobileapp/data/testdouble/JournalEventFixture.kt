package xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.AggregateTypeName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import java.nio.charset.StandardCharsets
import java.time.Instant

private const val MILLISECONDS_PER_EVENT = 1_000L

/** 造一条日志事件；载荷按 JSON 文本给，和线上信封一致。 */
internal fun journalEvent(
    eventIdentifier: String,
    aggregateIdentifier: String,
    sequenceNumber: Long,
    eventType: EventType,
    aggregateType: String = "history",
    payloadJsonText: String = "{}",
    metadata: Map<String, String> = emptyMap(),
): JournalEvent =
    JournalEvent(
        eventIdentifier = EventIdentifier(eventIdentifier),
        sequenceNumber = sequenceNumber,
        aggregateType = AggregateTypeName(aggregateType),
        aggregateIdentifier = AggregateIdentifier(aggregateIdentifier),
        eventType = eventType,
        eventVersion = 1,
        occurredAt = Instant.ofEpochMilli(sequenceNumber * MILLISECONDS_PER_EVENT),
        payload = payloadJsonText.toByteArray(StandardCharsets.UTF_8),
        metadata = metadata,
    )

/** 造一条「历史记录被观察到」事件；序号决定事件身份，方便按序号造一串连续事件。 */
internal fun historyItemObserved(
    historyIdentifier: String,
    sequenceNumber: Long,
    host: String = "api.example.com",
    method: String = "GET",
): JournalEvent =
    journalEvent(
        eventIdentifier = "event_$sequenceNumber",
        aggregateIdentifier = historyIdentifier,
        sequenceNumber = sequenceNumber,
        eventType = EventType.HISTORY_ITEM_OBSERVED,
        payloadJsonText = """{"host":"$host","method":"$method"}""",
    )

/** 造一条「历史记录已保存」事件。 */
internal fun historyItemSaved(
    historyIdentifier: String,
    sequenceNumber: Long,
): JournalEvent =
    journalEvent(
        eventIdentifier = "event_$sequenceNumber",
        aggregateIdentifier = historyIdentifier,
        sequenceNumber = sequenceNumber,
        eventType = EventType.HISTORY_ITEM_SAVED,
    )

/** 造一条「历史记录新增注解」事件。 */
internal fun historyAnnotationAdded(
    historyIdentifier: String,
    sequenceNumber: Long,
): JournalEvent =
    journalEvent(
        eventIdentifier = "event_$sequenceNumber",
        aggregateIdentifier = historyIdentifier,
        sequenceNumber = sequenceNumber,
        eventType = EventType.HISTORY_ANNOTATION_ADDED,
    )
