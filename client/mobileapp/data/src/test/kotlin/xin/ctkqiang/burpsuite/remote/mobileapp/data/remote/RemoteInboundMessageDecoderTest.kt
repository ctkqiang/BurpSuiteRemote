package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.AggregateType

class RemoteInboundMessageDecoderTest {
    @Test
    fun `an event frame is decoded into the wire envelope`() {
        val decoded = RemoteInboundMessageDecoder.decode(EVENT_FRAME)

        val eventReceived = assertInstanceOf(RemoteInboundMessage.EventReceived::class.java, decoded)
        assertEquals(EventIdentifier("event_1"), eventReceived.envelope.eventIdentifier)
        assertEquals(5L, eventReceived.envelope.sequenceNumber)
        assertEquals(EventType("history.item.observed"), eventReceived.envelope.eventType)
        assertEquals(AggregateType.History, eventReceived.envelope.aggregateType)
        assertEquals(AggregateIdentifier("history_1"), eventReceived.envelope.aggregateIdentifier)
    }

    @Test
    fun `a signal frame is decoded into its code`() {
        val decoded = RemoteInboundMessageDecoder.decode(SIGNAL_FRAME)

        val signalReceived = assertInstanceOf(RemoteInboundMessage.SignalReceived::class.java, decoded)
        assertEquals(RemoteConnectionSignalCode.AuthenticationSucceeded, signalReceived.code)
    }

    @Test
    fun `a frame with an unknown message type is malformed`() {
        assertEquals(
            RemoteInboundMessage.Malformed,
            RemoteInboundMessageDecoder.decode("""{"messageType":"unheard_of"}"""),
        )
    }

    @Test
    fun `a frame that is not json is malformed`() {
        assertEquals(RemoteInboundMessage.Malformed, RemoteInboundMessageDecoder.decode("not json"))
    }

    @Test
    fun `a signal with an unknown code is malformed rather than guessed`() {
        assertEquals(
            RemoteInboundMessage.Malformed,
            RemoteInboundMessageDecoder.decode(SIGNAL_FRAME_WITH_UNKNOWN_CODE),
        )
    }

    @Test
    fun `an event missing required fields is malformed`() {
        assertEquals(
            RemoteInboundMessage.Malformed,
            RemoteInboundMessageDecoder.decode("""{"protocolVersion":1,"messageType":"event"}"""),
        )
    }

    private companion object {
        const val EVENT_FRAME =
            """{"protocolVersion":1,"messageType":"event","eventIdentifier":"event_1",""" +
                """"sequenceNumber":5,"occurredAt":1757660000000,"eventType":"history.item.observed",""" +
                """"aggregateType":"history","aggregateIdentifier":"history_1",""" +
                """"payload":{"host":"api.example.com"}}"""

        const val SIGNAL_FRAME =
            """{"protocolVersion":1,"messageType":"signal","code":"authentication_succeeded"}"""

        const val SIGNAL_FRAME_WITH_UNKNOWN_CODE =
            """{"protocolVersion":1,"messageType":"signal","code":"brand_new_signal"}"""
    }
}
