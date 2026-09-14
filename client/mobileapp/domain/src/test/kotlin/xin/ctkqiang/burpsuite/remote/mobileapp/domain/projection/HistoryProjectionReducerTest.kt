package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.EventPayloadAttributes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import java.time.Instant

/**
 * 折历史事件的回归测试。
 *
 * 载荷字段名是两端共用的线上契约：插件按哪个名字写，读模型就得按哪个名字读。界面上的「—」正是这条链
 * 断了的样子，因此这里逐个字段钉住，改错常量会当场红掉，而不是等用户看到一片空白。
 */
class HistoryProjectionReducerTest {
    @Test
    fun `an observed event fills every metadata field of the read model`() {
        val record =
            reduceIntoReadModel(
                recordedEvent(
                    payloadAttributes =
                        attributesOf(
                            "host" to "chat.baidu.com",
                            "method" to "OPTIONS",
                            "scheme" to "https",
                            "path" to "/aichat/api/messages/list",
                            "statusCode" to "200",
                            "mimeType" to "application/json",
                            "responseLength" to "4096",
                            "usesTls" to "true",
                            "destinationInternetProtocolAddress" to "110.242.68.66",
                            "listenerPort" to "8443",
                            "durationMilliseconds" to "137",
                        ),
                ),
            )

        assertEquals(HISTORY_IDENTIFIER_VALUE, record.historyIdentifier.value)
        assertEquals(DEFAULT_SEQUENCE_NUMBER, record.sequenceNumber)
        assertEquals(OCCURRED_AT, record.occurredAt)
        assertEquals("chat.baidu.com", record.host)
        assertEquals("OPTIONS", record.method)
        assertEquals("https", record.scheme)
        assertEquals("/aichat/api/messages/list", record.path)
        assertEquals(200, record.statusCode)
        assertEquals("application/json", record.mimeType)
        assertEquals(4096L, record.responseLength)
        assertEquals(true, record.usesTls)
        assertEquals("110.242.68.66", record.destinationInternetProtocolAddress)
        assertEquals(8443, record.listenerPort)
        assertEquals(137L, record.durationMilliseconds)
    }

    @Test
    fun `a payload without metadata leaves the read model unknown instead of zero`() {
        val record = reduceIntoReadModel(recordedEvent(payloadAttributes = EventPayloadAttributes.EMPTY))

        assertNull(record.host)
        assertNull(record.method)
        assertNull(record.scheme)
        assertNull(record.path)
        assertNull(record.statusCode)
        assertNull(record.mimeType)
        assertNull(record.responseLength)
        assertNull(record.usesTls)
        assertNull(record.destinationInternetProtocolAddress)
        assertNull(record.listenerPort)
        assertNull(record.durationMilliseconds)
        assertNull(record.title)
        assertFalse(record.isEdited)
        assertEquals(HistoryArchiveState.Live, record.archiveState)
        assertEquals(0, record.annotationCount)
    }

    @Test
    fun `a field the event omits keeps the value an earlier event already set`() {
        val identifier = HistoryIdentifier(HISTORY_IDENTIFIER_VALUE)
        val stateAfterObservingHost =
            HistoryProjectionReducer.reduce(
                HistoryProjectionState(),
                recordedEvent(
                    payloadAttributes = attributesOf("host" to "chat.baidu.com", "method" to "OPTIONS"),
                    eventIdentifier = "event_1",
                    sequenceNumber = 1,
                ),
            )

        val stateAfterObservingStatus =
            HistoryProjectionReducer.reduce(
                stateAfterObservingHost,
                recordedEvent(
                    payloadAttributes = attributesOf("statusCode" to "200"),
                    eventIdentifier = "event_2",
                    sequenceNumber = 2,
                ),
            )

        val record = requireNotNull(stateAfterObservingStatus.recordsByIdentifier[identifier])
        assertEquals("chat.baidu.com", record.host)
        assertEquals("OPTIONS", record.method)
        assertEquals(200, record.statusCode)
        assertEquals(2L, record.sequenceNumber)
    }

    @Test
    fun `an event that belongs to no history record folds into nothing`() {
        val unrelatedEvent =
            recordedEvent(
                payloadAttributes = attributesOf("host" to "chat.baidu.com"),
                eventType = EventType.DEVICE_PAIRED,
            )

        val state = HistoryProjectionReducer.reduce(HistoryProjectionState(), unrelatedEvent)

        assertNull(HistoryProjectionReducer.affectedHistoryIdentifier(unrelatedEvent))
        assertTrue(state.recordsByIdentifier.isEmpty())
    }

    private fun reduceIntoReadModel(event: RecordedEvent): HistoryRecord {
        val state = HistoryProjectionReducer.reduce(HistoryProjectionState(), event)
        return requireNotNull(state.recordsByIdentifier[HistoryIdentifier(HISTORY_IDENTIFIER_VALUE)])
    }

    private fun recordedEvent(
        payloadAttributes: EventPayloadAttributes,
        eventType: EventType = EventType.HISTORY_ITEM_OBSERVED,
        eventIdentifier: String = "event_1",
        sequenceNumber: Long = DEFAULT_SEQUENCE_NUMBER,
    ): RecordedEvent =
        RecordedEvent(
            eventIdentifier = EventIdentifier(eventIdentifier),
            sequenceNumber = sequenceNumber,
            occurredAt = OCCURRED_AT,
            eventType = eventType,
            aggregateIdentifier = AggregateIdentifier(HISTORY_IDENTIFIER_VALUE),
            payloadAttributes = payloadAttributes,
        )

    private fun attributesOf(vararg fieldValues: Pair<String, String>): EventPayloadAttributes =
        EventPayloadAttributes(fieldValues.toMap())

    private companion object {
        const val HISTORY_IDENTIFIER_VALUE = "history_0123456789abcdef"
        const val DEFAULT_SEQUENCE_NUMBER = 2260L

        val OCCURRED_AT: Instant = Instant.parse("2026-09-14T02:27:00Z")
    }
}
