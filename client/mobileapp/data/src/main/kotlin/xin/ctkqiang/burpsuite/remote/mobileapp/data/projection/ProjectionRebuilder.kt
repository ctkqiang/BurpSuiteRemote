package xin.ctkqiang.burpsuite.remote.mobileapp.data.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventRecordedEventMapper
import xin.ctkqiang.burpsuite.remote.mobileapp.data.transaction.AtomicUnitOfWork
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.EventJournal
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.ProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionCheckpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider

/**
 * 投影重建。
 *
 * 从 0 号序号重放整份日志，把投影整份换掉并把校验点推到最新：投影与日志不一致时，
 * 这条路是唯一的修复方式（rules.md §5.6、§5.7）。
 */
class ProjectionRebuilder(
    private val eventJournal: EventJournal,
    private val projectionCheckpointStore: ProjectionCheckpointStore,
    private val recordedEventMapper: JournalEventRecordedEventMapper,
    private val atomicUnitOfWork: AtomicUnitOfWork,
    private val timeProvider: TimeProvider,
) {
    /** 重建一个投影；换内容与推校验点在同一个事务里，失败则投影保持原样。 */
    suspend fun rebuild(projectionStore: ProjectionStore) {
        val recordedEvents =
            eventJournal
                .readEventsAfter(EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER)
                .map { journalEvent -> recordedEventMapper.map(journalEvent) }
                .filter { recordedEvent -> projectionStore.handles(recordedEvent) }
        val lastSequenceNumber =
            recordedEvents.maxOfOrNull { recordedEvent -> recordedEvent.sequenceNumber }
                ?: EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER
        val checkpointUpdatedAt = timeProvider.now()

        atomicUnitOfWork.runAtomically {
            projectionStore.replaceAll(recordedEvents)
            projectionCheckpointStore.saveCheckpoint(
                ProjectionCheckpoint(
                    projectionName = projectionStore.projectionName,
                    lastSequenceNumber = lastSequenceNumber,
                    updatedAt = checkpointUpdatedAt,
                ),
            )
        }
    }

    private companion object {
        // 与领域侧口径一致：日志里没有比 0 更小的序号，重放从这里起步。
        const val EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER = 0L
    }
}
