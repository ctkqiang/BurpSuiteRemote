package xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.journalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.EventPayloadAttributes

class JournalEventRecordedEventMapperTest {
    @Test
    fun `a JSON payload is flattened into scalar attributes`() {
        val event =
            journalEvent(
                eventIdentifier = "event_1",
                aggregateIdentifier = "history_1",
                sequenceNumber = 1,
                eventType = EventType.HISTORY_ITEM_OBSERVED,
                payloadJsonText = """{"host":"api.example.com","statusCode":200,"usesTls":true}""",
            )

        val recordedEvent = JournalEventRecordedEventMapper.map(event)

        assertEquals(event.eventIdentifier, recordedEvent.eventIdentifier)
        assertEquals(event.sequenceNumber, recordedEvent.sequenceNumber)
        assertEquals("api.example.com", recordedEvent.payloadAttributes.text("host"))
        assertEquals(200, recordedEvent.payloadAttributes.integer("statusCode"))
        assertEquals(true, recordedEvent.payloadAttributes.boolean("usesTls"))
    }

    @Test
    fun `an unreadable payload yields no attributes instead of dropping the event`() {
        val event =
            journalEvent(
                eventIdentifier = "event_1",
                aggregateIdentifier = "history_1",
                sequenceNumber = 1,
                eventType = EventType.HISTORY_ITEM_OBSERVED,
                payloadJsonText = "not json at all",
            )

        val recordedEvent = JournalEventRecordedEventMapper.map(event)

        assertEquals(EventPayloadAttributes.EMPTY, recordedEvent.payloadAttributes)
    }

    @Test
    fun `nested structures are not surfaced as scalar attributes`() {
        val event =
            journalEvent(
                eventIdentifier = "event_1",
                aggregateIdentifier = "history_1",
                sequenceNumber = 1,
                eventType = EventType.HISTORY_ITEM_OBSERVED,
                payloadJsonText = """{"headers":{"host":"inside"},"tags":["a"],"host":"api.example.com"}""",
            )

        val recordedEvent = JournalEventRecordedEventMapper.map(event)

        assertEquals("api.example.com", recordedEvent.payloadAttributes.text("host"))
        assertNull(recordedEvent.payloadAttributes.text("headers"))
        assertNull(recordedEvent.payloadAttributes.text("tags"))
    }
}
