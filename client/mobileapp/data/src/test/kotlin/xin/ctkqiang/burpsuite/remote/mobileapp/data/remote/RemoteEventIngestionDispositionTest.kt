package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.EventIngestionOutcome

class RemoteEventIngestionDispositionTest {
    @Test
    fun `an applied event keeps the session streaming`() {
        assertNull(RemoteEventIngestionDisposition.sessionEndFor(EventIngestionOutcome.Applied))
    }

    @Test
    fun `a duplicate delivery keeps the session streaming`() {
        assertNull(RemoteEventIngestionDisposition.sessionEndFor(EventIngestionOutcome.AlreadyDelivered))
    }

    // 断洞必须重连接续传：静默继续会让投影永久漏掉一段事实（rules.md §5.5）。
    @Test
    fun `a detected gap demands a resume on a new connection`() {
        assertEquals(
            RemoteSessionEnd.ResumeRequired,
            RemoteEventIngestionDisposition.sessionEndFor(
                EventIngestionOutcome.ResynchronisationRequired(RESUME_AFTER_SEQUENCE_NUMBER),
            ),
        )
    }

    @Test
    fun `a reused sequence number ends the session as a contract mismatch`() {
        assertEquals(
            RemoteSessionEnd.ProtocolMismatch,
            RemoteEventIngestionDisposition.sessionEndFor(
                EventIngestionOutcome.SequenceNumberReused(REUSED_SEQUENCE_NUMBER),
            ),
        )
    }

    private companion object {
        const val RESUME_AFTER_SEQUENCE_NUMBER = 9L

        const val REUSED_SEQUENCE_NUMBER = 9L
    }
}
