package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.AggregateType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.EventEnvelope
import java.time.Instant

class RemoteHandshakeConclusionTest {
    @Test
    fun `an authentication success concludes the handshake as authenticated`() {
        assertEquals(
            RemoteHandshakeConclusion.Authenticated,
            RemoteHandshakeConclusion.forInboundMessage(
                RemoteInboundMessage.SignalReceived(RemoteConnectionSignalCode.AuthenticationSucceeded),
            ),
        )
    }

    @Test
    fun `an authentication failure concludes the handshake as rejected`() {
        val rejectedCodes =
            listOf(
                RemoteConnectionSignalCode.AuthenticationFailed,
                RemoteConnectionSignalCode.AuthenticationRequired,
            )

        for (signalCode in rejectedCodes) {
            assertEquals(
                RemoteHandshakeConclusion.AuthenticationRejected,
                RemoteHandshakeConclusion.forInboundMessage(RemoteInboundMessage.SignalReceived(signalCode)),
            )
        }
    }

    @Test
    fun `an unsupported protocol version is a contract mismatch`() {
        assertEquals(
            RemoteHandshakeConclusion.ProtocolMismatch,
            RemoteHandshakeConclusion.forInboundMessage(
                RemoteInboundMessage.SignalReceived(RemoteConnectionSignalCode.ProtocolVersionUnsupported),
            ),
        )
    }

    @Test
    fun `a malformed frame is a contract mismatch`() {
        assertEquals(
            RemoteHandshakeConclusion.ProtocolMismatch,
            RemoteHandshakeConclusion.forInboundMessage(RemoteInboundMessage.Malformed),
        )
    }

    // 认证之前插件不推事件；真收到了也不构成结论，否则会把噪声当成一次成功的握手。
    @Test
    fun `an event frame does not conclude the handshake`() {
        assertNull(
            RemoteHandshakeConclusion.forInboundMessage(RemoteInboundMessage.EventReceived(EVENT_ENVELOPE)),
        )
    }

    @Test
    fun `a resume phase signal does not conclude the handshake`() {
        val resumePhaseCodes =
            listOf(
                RemoteConnectionSignalCode.EventsNoLongerAvailable,
                RemoteConnectionSignalCode.SnapshotRequired,
            )

        for (signalCode in resumePhaseCodes) {
            assertNull(
                RemoteHandshakeConclusion.forInboundMessage(RemoteInboundMessage.SignalReceived(signalCode)),
            )
        }
    }

    private companion object {
        val EVENT_ENVELOPE =
            EventEnvelope(
                protocolVersion = 1,
                eventIdentifier = EventIdentifier("event_1"),
                sequenceNumber = 5,
                occurredAt = Instant.ofEpochMilli(1_757_660_000_000L),
                eventType = EventType("history.item.observed"),
                aggregateType = AggregateType.History,
                aggregateIdentifier = AggregateIdentifier("history_1"),
                payload = JsonObject(emptyMap()),
            )
    }
}
