package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import java.time.Instant

class StoredEventSerializationTest {
    @Test
    fun `a stored event survives a serialization round trip`() {
        val storedEvent =
            StoredEvent(
                eventIdentifier = EventIdentifier("event_01J"),
                sequenceNumber = 5003L,
                aggregate = AggregateReference(AggregateType.Intercept, AggregateIdentifier("intercept_123")),
                eventType = EventType.INTERCEPT_FORWARDED,
                eventVersion = 1,
                timestamp = Instant.ofEpochMilli(1757660000000L),
                payload = byteArrayOf(1, 2, 3),
                metadata = EventMetadata(mapOf("source" to "burp")),
            )

        val encodedEvent = json.encodeToString(StoredEvent.serializer(), storedEvent)
        val restoredEvent = json.decodeFromString(StoredEvent.serializer(), encodedEvent)

        assertEquals(storedEvent.eventIdentifier, restoredEvent.eventIdentifier)
        assertEquals(storedEvent.sequenceNumber, restoredEvent.sequenceNumber)
        assertEquals(storedEvent.aggregate, restoredEvent.aggregate)
        assertEquals(storedEvent.eventType, restoredEvent.eventType)
        assertEquals(storedEvent.eventVersion, restoredEvent.eventVersion)
        assertEquals(storedEvent.timestamp, restoredEvent.timestamp)
        assertEquals(storedEvent.metadata, restoredEvent.metadata)
        // ByteArray 的 equals 是引用比较，比内容必须显式调用 contentEquals
        assertTrue(storedEvent.payload.contentEquals(restoredEvent.payload))
    }

    private companion object {
        private val json = Json
    }
}
