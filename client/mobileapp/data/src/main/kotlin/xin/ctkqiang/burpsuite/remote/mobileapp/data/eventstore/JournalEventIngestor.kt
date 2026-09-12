package xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore

import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
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
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    /**
     * 收下一条事件。
     *
     * [synchronisationCoordinator] 只记内存里的进度，装配时必须以日志的最新序号为初值：
     * 否则重启后会把「已经收到过」误判成断洞。
     */
    suspend fun ingest(event: JournalEvent): EventIngestionOutcome {
        val outcome = accept(event)
        // 只记序号与结论：事件载荷属于用户数据，不进日志（rules.md §12）。
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.EventIngestion,
                message = outcome.description,
                attributes = mapOf("sequenceNumber" to event.sequenceNumber.toString()),
            ),
        )
        return outcome
    }

    private suspend fun accept(event: JournalEvent): EventIngestionOutcome =
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

// 结论要说成人话：光看「已丢弃」，分不出是重复投递还是断洞。
private val EventIngestionOutcome.description: String
    get() =
        when (this) {
            EventIngestionOutcome.Applied -> "事件已落盘并折入投影"
            EventIngestionOutcome.AlreadyDelivered -> "重复投递，已丢弃"
            is EventIngestionOutcome.ResynchronisationRequired -> "序号断洞，需从基准续传"
            is EventIngestionOutcome.SequenceNumberReused -> "序号被另一条事件复用，拒收"
        }
