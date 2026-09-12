package xin.ctkqiang.burpsuite.remote.mobileapp.domain.sync

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ResumptionSequenceNumberProvider
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ResynchronisationRecorder

/**
 * 事件同步协调器。
 *
 * 判定每条到货事件该被接受、视为重复，还是因为序号断洞而必须先续传（rules.md §5.5）。
 * 它同时是续传基准与快照记账的持有人：这两件事说的是同一个数字，分成两个对象迟早会走偏。
 */
class EventSynchronisationCoordinator(
    initialSequenceNumber: Long = EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER,
) : ResumptionSequenceNumberProvider, ResynchronisationRecorder {
    private val stateLock = Any()

    private var lastAcceptedSequenceNumber = initialSequenceNumber

    // 记住序号到身份的对应，才能把「重复投递」与「序号被复用」区分开；
    // 快照到达后清空：快照已经覆盖过的那些序号，其身份不再需要本地记着。
    private var acceptedIdentifiersBySequenceNumber = emptyMap<Long, EventIdentifier>()

    /** 判定一条到货事件；接受与否由调用方照结论执行，协调器自己不落盘。 */
    fun accept(event: JournalEvent): EventSynchronisationDecision =
        synchronized(stateLock) {
            if (isAlreadyAccepted(event.eventIdentifier)) {
                return EventSynchronisationDecision.Duplicate
            }

            when {
                event.sequenceNumber <= lastAcceptedSequenceNumber ->
                    decideForAlreadyCoveredSequenceNumber(event)
                event.sequenceNumber == lastAcceptedSequenceNumber + 1 ->
                    acceptContiguousEvent(event)
                else -> EventSynchronisationDecision.GapDetected(lastAcceptedSequenceNumber)
            }
        }

    override suspend fun currentSequenceNumber(): Long = synchronized(stateLock) { lastAcceptedSequenceNumber }

    override suspend fun recordSnapshot(snapshotSequenceNumber: Long) {
        markResynchronised(snapshotSequenceNumber)
    }

    /** 快照到达：基线推到快照那一刻，并忘掉快照已经覆盖的序号身份。 */
    fun markResynchronised(snapshotSequenceNumber: Long) {
        synchronized(stateLock) {
            lastAcceptedSequenceNumber = snapshotSequenceNumber
            acceptedIdentifiersBySequenceNumber = emptyMap()
        }
    }

    private fun isAlreadyAccepted(eventIdentifier: EventIdentifier): Boolean =
        acceptedIdentifiersBySequenceNumber.containsValue(eventIdentifier)

    private fun decideForAlreadyCoveredSequenceNumber(event: JournalEvent): EventSynchronisationDecision {
        val acceptedIdentifier = acceptedIdentifiersBySequenceNumber[event.sequenceNumber]
        if (acceptedIdentifier == null || acceptedIdentifier == event.eventIdentifier) {
            return EventSynchronisationDecision.Duplicate
        }

        return EventSynchronisationDecision.SequenceNumberReused(event.sequenceNumber)
    }

    private fun acceptContiguousEvent(event: JournalEvent): EventSynchronisationDecision {
        lastAcceptedSequenceNumber = event.sequenceNumber
        acceptedIdentifiersBySequenceNumber =
            acceptedIdentifiersBySequenceNumber + (event.sequenceNumber to event.eventIdentifier)

        return EventSynchronisationDecision.Accepted
    }

    private companion object {
        // 空日志的最新序号是 0，与插件侧 RemoteEventStream 的口径一致，于是「从 0 续传」永远成立。
        private const val EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER = 0L
    }
}
