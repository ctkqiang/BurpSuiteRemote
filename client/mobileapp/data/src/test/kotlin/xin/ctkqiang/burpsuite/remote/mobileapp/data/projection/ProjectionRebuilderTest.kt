package xin.ctkqiang.burpsuite.remote.mobileapp.data.projection

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventIngestor
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventRecordedEventMapper
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.InMemoryEventJournal
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.InMemoryHistoryProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.InMemoryProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.RollingBackAtomicUnitOfWork
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.historyAnnotationAdded
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.historyItemObserved
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.historyItemSaved
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.sync.EventSynchronisationCoordinator
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import java.time.Instant

class ProjectionRebuilderTest {
    private val eventJournal = InMemoryEventJournal()
    private val checkpointStore = InMemoryProjectionCheckpointStore()
    private val atomicUnitOfWork = RollingBackAtomicUnitOfWork(eventJournal, checkpointStore)
    private val timeProvider = TimeProvider { FIXED_INSTANT }

    @Test
    fun `an empty projection rebuilt from the journal equals the incrementally built one`() =
        runTest {
            val incrementalStore = InMemoryHistoryProjectionStore()
            val ingestor =
                JournalEventIngestor(
                    eventJournal = eventJournal,
                    projectionCheckpointStore = checkpointStore,
                    projectionStores = listOf(incrementalStore),
                    synchronisationCoordinator = EventSynchronisationCoordinator(),
                    recordedEventMapper = JournalEventRecordedEventMapper,
                    atomicUnitOfWork = atomicUnitOfWork,
                    timeProvider = timeProvider,
                )
            val events =
                listOf(
                    historyItemObserved(historyIdentifier = "history_1", sequenceNumber = 1),
                    historyItemObserved(historyIdentifier = "history_2", sequenceNumber = 2),
                    historyItemSaved(historyIdentifier = "history_1", sequenceNumber = 3),
                    historyAnnotationAdded(historyIdentifier = "history_2", sequenceNumber = 4),
                )
            for (event in events) {
                ingestor.ingest(event)
            }

            // 空库重建：不带任何既有投影内容，只从 0 号序号重放整份日志。
            val rebuiltStore = InMemoryHistoryProjectionStore()
            ProjectionRebuilder(
                eventJournal = eventJournal,
                projectionCheckpointStore = checkpointStore,
                recordedEventMapper = JournalEventRecordedEventMapper,
                atomicUnitOfWork = atomicUnitOfWork,
                timeProvider = timeProvider,
            ).rebuild(rebuiltStore)

            assertEquals(incrementalStore.currentRecords(), rebuiltStore.currentRecords())
            assertEquals(4L, checkpointStore.readCheckpoint(ProjectionName.HISTORY)?.lastSequenceNumber)
        }

    private companion object {
        val FIXED_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
