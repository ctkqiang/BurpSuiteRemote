package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.AggregateType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.EventEnvelope
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.AggregateTypeName
import java.nio.charset.StandardCharsets
import java.time.Instant

class RemoteJournalEventMapperTest {
    @Test
    fun `a wire envelope becomes a journal event with its payload preserved`() {
        val envelope =
            EventEnvelope(
                protocolVersion = 1,
                eventIdentifier = EventIdentifier("event_1"),
                sequenceNumber = 5,
                occurredAt = OCCURRED_AT,
                eventType = EventType("history.item.observed"),
                aggregateType = AggregateType.History,
                aggregateIdentifier = AggregateIdentifier("history_1"),
                payload = Json.parseToJsonElement(PAYLOAD_JSON_TEXT).jsonObject,
            )

        val journalEvent = RemoteJournalEventMapper.map(envelope)

        assertEquals(EventIdentifier("event_1"), journalEvent.eventIdentifier)
        assertEquals(5L, journalEvent.sequenceNumber)
        assertEquals(AggregateTypeName("history"), journalEvent.aggregateType)
        assertEquals(AggregateIdentifier("history_1"), journalEvent.aggregateIdentifier)
        assertEquals(EventType("history.item.observed"), journalEvent.eventType)
        assertEquals(1, journalEvent.eventVersion)
        assertEquals(OCCURRED_AT, journalEvent.occurredAt)
        assertEquals(PAYLOAD_JSON_TEXT, String(journalEvent.payload, StandardCharsets.UTF_8))
        assertTrue(journalEvent.metadata.isEmpty())
    }

    private companion object {
        const val PAYLOAD_JSON_TEXT = """{"host":"api.example.com"}"""
        val OCCURRED_AT: Instant = Instant.ofEpochMilli(1_757_660_000_000L)
    }
}
