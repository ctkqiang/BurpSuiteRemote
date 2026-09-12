package xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.InMemoryEventJournal
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.InMemoryHistoryProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.InMemoryProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.RollingBackAtomicUnitOfWork
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.historyItemObserved
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.journalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.sync.EventSynchronisationCoordinator
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import java.time.Instant

class JournalEventIngestorTest {
    private val eventJournal = InMemoryEventJournal()
    private val checkpointStore = InMemoryProjectionCheckpointStore()
    private val projectionStore = InMemoryHistoryProjectionStore()

    private fun ingestorSeededFrom(
        synchronisationCoordinator: EventSynchronisationCoordinator = EventSynchronisationCoordinator(),
    ): JournalEventIngestor =
        JournalEventIngestor(
            eventJournal = eventJournal,
            projectionCheckpointStore = checkpointStore,
            projectionStores = listOf(projectionStore),
            synchronisationCoordinator = synchronisationCoordinator,
            recordedEventMapper = JournalEventRecordedEventMapper,
            atomicUnitOfWork = RollingBackAtomicUnitOfWork(eventJournal, checkpointStore),
            timeProvider = TimeProvider { FIXED_INSTANT },
        )

    @Test
    fun `the same event delivered twice leaves the projection untouched`() =
        runTest {
            val ingestor = ingestorSeededFrom()
            val event = historyItemObserved(historyIdentifier = "history_1", sequenceNumber = 1)

            val firstOutcome = ingestor.ingest(event)
            val secondOutcome = ingestor.ingest(event)

            assertEquals(EventIngestionOutcome.Applied, firstOutcome)
            assertEquals(EventIngestionOutcome.AlreadyDelivered, secondOutcome)
            assertEquals(1, projectionStore.currentRecords().size)
            assertEquals(1L, eventJournal.latestSequenceNumber())
            assertEquals(1L, checkpointStore.readCheckpoint(ProjectionName.HISTORY)?.lastSequenceNumber)
        }

    @Test
    fun `a missing sequence number demands a resume instead of continuing`() =
        runTest {
            val ingestor = ingestorSeededFrom()
            ingestor.ingest(historyItemObserved(historyIdentifier = "history_1", sequenceNumber = 1))

            val outcome = ingestor.ingest(historyItemObserved(historyIdentifier = "history_2", sequenceNumber = 3))

            assertEquals(EventIngestionOutcome.ResynchronisationRequired(1L), outcome)
            assertEquals(1, projectionStore.currentRecords().size)
            assertEquals(1L, eventJournal.latestSequenceNumber())
            assertEquals(1L, checkpointStore.readCheckpoint(ProjectionName.HISTORY)?.lastSequenceNumber)
        }

    @Test
    fun `a sequence number reused by another event is rejected`() =
        runTest {
            val ingestor = ingestorSeededFrom()
            ingestor.ingest(historyItemObserved(historyIdentifier = "history_1", sequenceNumber = 1))

            val outcome =
                ingestor.ingest(
                    journalEvent(
                        eventIdentifier = "event_other",
                        aggregateIdentifier = "history_2",
                        sequenceNumber = 1,
                        eventType = EventType.HISTORY_ITEM_OBSERVED,
                    ),
                )

            assertEquals(EventIngestionOutcome.SequenceNumberReused(1L), outcome)
            assertEquals(1, projectionStore.currentRecords().size)
        }

    @Test
    fun `an event and its checkpoint are committed together or not at all`() =
        runTest {
            val ingestor = ingestorSeededFrom()
            projectionStore.applyFailure = IllegalStateException("projection write failed")

            val failure =
                try {
                    ingestor.ingest(historyItemObserved(historyIdentifier = "history_1", sequenceNumber = 1))
                    null
                } catch (exception: IllegalStateException) {
                    exception
                }

            assertNotNull(failure)
            assertTrue(projectionStore.currentRecords().isEmpty())
            assertEquals(0L, eventJournal.latestSequenceNumber())
            assertNull(checkpointStore.readCheckpoint(ProjectionName.HISTORY))
        }

    @Test
    fun `a coordinator seeded from the journal still recognises a re-delivered event`() =
        runTest {
            val event = historyItemObserved(historyIdentifier = "history_1", sequenceNumber = 1)
            ingestorSeededFrom().ingest(event)

            // 重启：新协调器以日志最新序号为初值，重复投递仍必须判成已收到，而不是断洞。
            val restartedIngestor =
                ingestorSeededFrom(EventSynchronisationCoordinator(eventJournal.latestSequenceNumber()))

            assertEquals(EventIngestionOutcome.AlreadyDelivered, restartedIngestor.ingest(event))
            assertEquals(1, projectionStore.currentRecords().size)
        }

    private companion object {
        val FIXED_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
