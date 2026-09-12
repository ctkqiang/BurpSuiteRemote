package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RemoteSignalDispositionTest {
    @Test
    fun `an authentication success keeps the session streaming`() {
        assertEquals(
            RemoteSignalDisposition.Continue,
            RemoteSignalDisposition.forSignalCode(RemoteConnectionSignalCode.AuthenticationSucceeded),
        )
    }

    // 区间不可用与要快照是同一件事的两个出口：取一份快照，再带着新基准续传（plan §10）。
    @Test
    fun `a no longer available range leads to a snapshot and a fresh resume`() {
        val resynchronisationCodes =
            listOf(
                RemoteConnectionSignalCode.EventsNoLongerAvailable,
                RemoteConnectionSignalCode.SnapshotRequired,
            )

        for (signalCode in resynchronisationCodes) {
            assertEquals(
                RemoteSignalDisposition.ResynchroniseThenResume,
                RemoteSignalDisposition.forSignalCode(signalCode),
            )
        }
    }

    @Test
    fun `an authentication failure ends the session as rejected`() {
        val rejectedCodes =
            listOf(
                RemoteConnectionSignalCode.AuthenticationFailed,
                RemoteConnectionSignalCode.AuthenticationRequired,
            )

        for (signalCode in rejectedCodes) {
            assertEquals(
                RemoteSignalDisposition.EndSession(RemoteSessionEnd.AuthenticationRejected),
                RemoteSignalDisposition.forSignalCode(signalCode),
            )
        }
    }

    @Test
    fun `an unsupported protocol version ends the session as a contract mismatch`() {
        assertEquals(
            RemoteSignalDisposition.EndSession(RemoteSessionEnd.ProtocolMismatch),
            RemoteSignalDisposition.forSignalCode(RemoteConnectionSignalCode.ProtocolVersionUnsupported),
        )
    }
}
