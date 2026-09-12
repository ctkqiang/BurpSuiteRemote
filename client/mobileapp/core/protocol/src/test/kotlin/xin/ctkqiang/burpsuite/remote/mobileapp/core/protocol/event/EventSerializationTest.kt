package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.event

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import java.time.Instant

class EventSerializationTest {
    @Test
    fun `DeviceConnected survives a serialization round trip`() {
        val event =
            DeviceConnected(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(DeviceConnected.serializer(), event))
        assertEquals(EventType.DEVICE_CONNECTED, event.eventType)
    }

    @Test
    fun `DeviceDisconnected survives a serialization round trip`() {
        val event =
            DeviceDisconnected(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(DeviceDisconnected.serializer(), event))
        assertEquals(EventType.DEVICE_DISCONNECTED, event.eventType)
    }

    @Test
    fun `DevicePaired survives a serialization round trip`() {
        val event =
            DevicePaired(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(DevicePaired.serializer(), event))
        assertEquals(EventType.DEVICE_PAIRED, event.eventType)
    }

    @Test
    fun `DeviceUnpaired survives a serialization round trip`() {
        val event =
            DeviceUnpaired(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(DeviceUnpaired.serializer(), event))
        assertEquals(EventType.DEVICE_UNPAIRED, event.eventType)
    }

    @Test
    fun `HistoryItemObserved survives a serialization round trip`() {
        val event =
            HistoryItemObserved(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(HistoryItemObserved.serializer(), event))
        assertEquals(EventType.HISTORY_ITEM_OBSERVED, event.eventType)
    }

    @Test
    fun `HistoryItemSaved survives a serialization round trip`() {
        val event =
            HistoryItemSaved(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(HistoryItemSaved.serializer(), event))
        assertEquals(EventType.HISTORY_ITEM_SAVED, event.eventType)
    }

    @Test
    fun `HistoryAnnotationAdded survives a serialization round trip`() {
        val event =
            HistoryAnnotationAdded(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(HistoryAnnotationAdded.serializer(), event))
        assertEquals(EventType.HISTORY_ANNOTATION_ADDED, event.eventType)
    }

    @Test
    fun `InterceptCreated survives a serialization round trip`() {
        val event =
            InterceptCreated(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(InterceptCreated.serializer(), event))
        assertEquals(EventType.INTERCEPT_CREATED, event.eventType)
    }

    @Test
    fun `InterceptModified survives a serialization round trip`() {
        val event =
            InterceptModified(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(InterceptModified.serializer(), event))
        assertEquals(EventType.INTERCEPT_MODIFIED, event.eventType)
    }

    @Test
    fun `InterceptForwarded survives a serialization round trip`() {
        val event =
            InterceptForwarded(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(InterceptForwarded.serializer(), event))
        assertEquals(EventType.INTERCEPT_FORWARDED, event.eventType)
    }

    @Test
    fun `InterceptDropped survives a serialization round trip`() {
        val event =
            InterceptDropped(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(InterceptDropped.serializer(), event))
        assertEquals(EventType.INTERCEPT_DROPPED, event.eventType)
    }

    @Test
    fun `RepeaterCreated survives a serialization round trip`() {
        val event =
            RepeaterCreated(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(RepeaterCreated.serializer(), event))
        assertEquals(EventType.REPEATER_CREATED, event.eventType)
    }

    @Test
    fun `RepeaterExecutionStarted survives a serialization round trip`() {
        val event =
            RepeaterExecutionStarted(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(RepeaterExecutionStarted.serializer(), event))
        assertEquals(EventType.REPEATER_EXECUTION_STARTED, event.eventType)
    }

    @Test
    fun `RepeaterExecutionCompleted survives a serialization round trip`() {
        val event =
            RepeaterExecutionCompleted(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(RepeaterExecutionCompleted.serializer(), event))
        assertEquals(EventType.REPEATER_EXECUTION_COMPLETED, event.eventType)
    }

    @Test
    fun `ScreenshotImported survives a serialization round trip`() {
        val event =
            ScreenshotImported(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(ScreenshotImported.serializer(), event))
        assertEquals(EventType.SCREENSHOT_IMPORTED, event.eventType)
    }

    @Test
    fun `ScreenshotBeautified survives a serialization round trip`() {
        val event =
            ScreenshotBeautified(
                eventIdentifier = EVENT_IDENTIFIER,
                sequenceNumber = SEQUENCE_NUMBER,
                occurredAt = OCCURRED_AT,
            )

        assertEquals(event, roundTrip(ScreenshotBeautified.serializer(), event))
        assertEquals(EventType.SCREENSHOT_BEAUTIFIED, event.eventType)
    }

    // 编解码共用同一个 Json 实例：两边配置不同会掩盖契约问题
    private fun <T> roundTrip(
        serializer: KSerializer<T>,
        value: T,
    ): T = json.decodeFromString(serializer, json.encodeToString(serializer, value))

    private companion object {
        private val json = Json

        private val EVENT_IDENTIFIER = EventIdentifier("event_01J")

        private const val SEQUENCE_NUMBER = 5003L

        private val OCCURRED_AT: Instant = Instant.ofEpochMilli(1757660000000L)
    }
}
