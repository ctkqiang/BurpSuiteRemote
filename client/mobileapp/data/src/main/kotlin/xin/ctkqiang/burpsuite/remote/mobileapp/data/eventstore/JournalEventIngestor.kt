package xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore

import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.ProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.transaction.AtomicUnitOfWork
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.EventJournal
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.ProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionCheckpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.sync.EventSynchronisationCoordinator
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.sync.EventSynchronisationDecision
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider

/**
 * 事件入账。
 *
 * 先由事件同步协调器判定重复与断洞，再在一个事务里落事件、折投影、推校验点：
 * 三者要么都成、要么都不成（rules.md §5.4、plan §64、plan §65）。
 */
class JournalEventIngestor(
    private val eventJournal: EventJournal,
    private val projectionCheckpointStore: ProjectionCheckpointStore,
    private val projectionStores: List<ProjectionStore>,
    private val synchronisationCoordinator: EventSynchronisationCoordinator,
    private val recordedEventMapper: JournalEventRecordedEventMapper,
    private val atomicUnitOfWork: AtomicUnitOfWork,
    private val timeProvider: TimeProvider,
) {
    /**
     * 收下一条事件。
     *
     * [synchronisationCoordinator] 只记内存里的进度，装配时必须以日志的最新序号为初值：
     * 否则重启后会把「已经收到过」误判成断洞。
     */
    suspend fun ingest(event: JournalEvent): EventIngestionOutcome =
        when (val decision = synchronisationCoordinator.accept(event)) {
            EventSynchronisationDecision.Duplicate -> EventIngestionOutcome.AlreadyDelivered
            is EventSynchronisationDecision.GapDetected ->
                EventIngestionOutcome.ResynchronisationRequired(decision.resumeAfterSequenceNumber)
            is EventSynchronisationDecision.SequenceNumberReused ->
                EventIngestionOutcome.SequenceNumberReused(decision.sequenceNumber)
            EventSynchronisationDecision.Accepted -> {
                atomicUnitOfWork.runAtomically {
                    eventJournal.appendEvents(listOf(event))
                    applyToProjections(recordedEventMapper.map(event))
                }
                EventIngestionOutcome.Applied
            }
        }

    // 投影与校验点必须与事件在同一个事务里推进，否则会出现「事件在、投影没跟上」（plan §65）。
    private suspend fun applyToProjections(recordedEvent: RecordedEvent) {
        val checkpointUpdatedAt = timeProvider.now()
        for (projectionStore in projectionStores) {
            if (projectionStore.handles(recordedEvent)) {
                projectionStore.apply(recordedEvent)
                projectionCheckpointStore.saveCheckpoint(
                    ProjectionCheckpoint(
                        projectionName = projectionStore.projectionName,
                        lastSequenceNumber = recordedEvent.sequenceNumber,
                        updatedAt = checkpointUpdatedAt,
                    ),
                )
            }
        }
    }
}
