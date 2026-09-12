package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import java.time.Instant

class EventEnvelopeSerializationTest {
    @Test
    fun `an envelope reads occurredAt as an epoch millisecond integer`() {
        val envelope = decode(CANONICAL_ENVELOPE)

        assertEquals(Instant.ofEpochMilli(OCCURRED_AT_EPOCH_MILLISECONDS), envelope.occurredAt)
        assertEquals(OCCURRED_AT_EPOCH_MILLISECONDS, envelope.occurredAt.toEpochMilli())
        assertEquals(SEQUENCE_NUMBER, envelope.sequenceNumber)
        assertEquals(EventIdentifier(EVENT_IDENTIFIER_TEXT), envelope.eventIdentifier)
        assertEquals(EventType.INTERCEPT_FORWARDED, envelope.eventType)
        assertEquals(AggregateType.Intercept, envelope.aggregateType)
        assertEquals(AggregateIdentifier(AGGREGATE_IDENTIFIER_TEXT), envelope.aggregateIdentifier)
        assertTrue(envelope.payload.isEmpty())
    }

    @Test
    fun `an envelope writes occurredAt as a bare number`() {
        val envelope = decode(CANONICAL_ENVELOPE)

        val encodedEnvelope = json.encodeToString(EventEnvelope.serializer(), envelope)

        assertTrue(encodedEnvelope.contains("\"occurredAt\":$OCCURRED_AT_EPOCH_MILLISECONDS"))
    }

    @Test
    fun `an ISO 8601 occurredAt is rejected because the contract asks for milliseconds`() {
        assertThrows<SerializationException> { decode(ENVELOPE_WITH_ISO_TEXT_OCCURRED_AT) }
    }

    @Test
    fun `an unknown eventType is kept verbatim instead of dropping the fact`() {
        val envelope = decode(ENVELOPE_WITH_UNKNOWN_EVENT_TYPE)

        assertEquals(UNKNOWN_EVENT_TYPE_TEXT, envelope.eventType.value)
    }

    @Test
    fun `an unknown aggregateType is rejected because that set is closed`() {
        assertThrows<SerializationException> { decode(ENVELOPE_WITH_UNKNOWN_AGGREGATE_TYPE) }
    }

    @Test
    fun `an envelope survives a serialization round trip`() {
        val envelope = decode(CANONICAL_ENVELOPE)

        val encodedEnvelope = json.encodeToString(EventEnvelope.serializer(), envelope)
        val restoredEnvelope = json.decodeFromString(EventEnvelope.serializer(), encodedEnvelope)

        assertEquals(envelope, restoredEnvelope)
    }

    private fun decode(encodedEnvelopeText: String): EventEnvelope =
        json.decodeFromString(EventEnvelope.serializer(), encodedEnvelopeText)

    private companion object {
        private val json = Json

        private const val OCCURRED_AT_EPOCH_MILLISECONDS = 1757660000000L

        private const val SEQUENCE_NUMBER = 5003L

        private const val EVENT_IDENTIFIER_TEXT = "event_01J"

        private const val AGGREGATE_IDENTIFIER_TEXT = "intercept_123"

        private const val UNKNOWN_EVENT_TYPE_TEXT = "future.something.happened"

        // 用 plan §8 的信封原文，occurredAt 故意不写引号：它必须是数字
        private val CANONICAL_ENVELOPE =
            """
            {
              "protocolVersion": 1,
              "eventIdentifier": "$EVENT_IDENTIFIER_TEXT",
              "sequenceNumber": 5003,
              "occurredAt": 1757660000000,
              "eventType": "intercept.forwarded",
              "aggregateType": "intercept",
              "aggregateIdentifier": "$AGGREGATE_IDENTIFIER_TEXT",
              "payload": {}
            }
            """.trimIndent()

        private val ENVELOPE_WITH_ISO_TEXT_OCCURRED_AT =
            CANONICAL_ENVELOPE.replace(
                "\"occurredAt\": $OCCURRED_AT_EPOCH_MILLISECONDS",
                "\"occurredAt\": \"2025-09-12T07:33:20Z\"",
            )

        private val ENVELOPE_WITH_UNKNOWN_EVENT_TYPE =
            CANONICAL_ENVELOPE.replace("intercept.forwarded", UNKNOWN_EVENT_TYPE_TEXT)

        private val ENVELOPE_WITH_UNKNOWN_AGGREGATE_TYPE =
            CANONICAL_ENVELOPE.replace("\"aggregateType\": \"intercept\"", "\"aggregateType\": \"future_aggregate\"")
    }
}
