package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventRecordedEventMapper
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.ProjectionRebuilder
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.ProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.InMemoryEventJournal
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.InMemoryProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble.RollingBackAtomicUnitOfWork
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteRuntimeState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.sync.EventSynchronisationCoordinator
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import java.time.Instant

class RemoteResynchronisationTest {
    private val eventJournal = InMemoryEventJournal()
    private val checkpointStore = InMemoryProjectionCheckpointStore()

    @Test
    fun `a successful snapshot advances the resume baseline and rebuilds every projection`() =
        runTest {
            val projectionStore = RecordingProjectionStore()
            val synchronisationCoordinator = EventSynchronisationCoordinator()

            val result =
                resynchronisation(projectionStore, synchronisationCoordinator, RemoteResult.Succeeded(SNAPSHOT))
                    .resynchronise(CONFIGURATION)

            assertEquals(RemoteResult.Succeeded(SNAPSHOT), result)
            assertEquals(SNAPSHOT.latestEventSequenceNumber, synchronisationCoordinator.currentSequenceNumber())
            assertEquals(1, projectionStore.replaceAllCallCount)
        }

    @Test
    fun `a failed snapshot changes neither the baseline nor the projections`() =
        runTest {
            val projectionStore = RecordingProjectionStore()
            val synchronisationCoordinator = EventSynchronisationCoordinator(SEEDED_SEQUENCE_NUMBER)
            val snapshotFailure = RemoteResult.Failed(RemoteFailure.ServerUnavailable)

            val result =
                resynchronisation(projectionStore, synchronisationCoordinator, snapshotFailure)
                    .resynchronise(CONFIGURATION)

            assertEquals(snapshotFailure, result)
            assertEquals(SEEDED_SEQUENCE_NUMBER, synchronisationCoordinator.currentSequenceNumber())
            assertEquals(0, projectionStore.replaceAllCallCount)
        }

    private fun resynchronisation(
        projectionStore: RecordingProjectionStore,
        synchronisationCoordinator: EventSynchronisationCoordinator,
        snapshotResult: RemoteResult<RemoteRuntimeState>,
    ): RemoteResynchronisation =
        RemoteResynchronisation(
            snapshotSource = RemoteSnapshotSource { snapshotResult },
            resynchronisationRecorder = synchronisationCoordinator,
            projectionRebuilder =
                ProjectionRebuilder(
                    eventJournal = eventJournal,
                    projectionCheckpointStore = checkpointStore,
                    recordedEventMapper = JournalEventRecordedEventMapper,
                    atomicUnitOfWork = RollingBackAtomicUnitOfWork(eventJournal, checkpointStore),
                    timeProvider = TimeProvider { FIXED_INSTANT },
                ),
            projectionStores = listOf(projectionStore),
        )

    // 只关心「重建有没有被调用」，因此事件本身不需要真的折入。
    private class RecordingProjectionStore : ProjectionStore {
        var replaceAllCallCount = 0

        override val projectionName: ProjectionName = ProjectionName.HISTORY

        override fun handles(event: RecordedEvent): Boolean = true

        override suspend fun apply(event: RecordedEvent) = Unit

        override suspend fun replaceAll(events: List<RecordedEvent>) {
            replaceAllCallCount += 1
        }
    }

    private companion object {
        const val SEEDED_SEQUENCE_NUMBER = 9L

        val FIXED_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")

        val CONFIGURATION =
            RemoteConnectionConfiguration(host = "127.0.0.1", port = 9000, deviceName = "test-device")

        val SNAPSHOT =
            RemoteRuntimeState(
                protocolVersion = 1,
                remotePort = 9000,
                connectedDeviceCount = 1,
                pairedDeviceCount = 1,
                latestEventSequenceNumber = 42L,
            )
    }
}
