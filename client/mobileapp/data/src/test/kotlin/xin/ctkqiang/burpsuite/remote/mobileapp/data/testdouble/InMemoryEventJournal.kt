package xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.EventJournal

// 内存事件日志；带快照与回滚，用来在纯 JVM 上验证事务语义。
internal class InMemoryEventJournal : EventJournal {
    private var storedEvents: List<JournalEvent> = emptyList()

    override suspend fun appendEvents(events: List<JournalEvent>) {
        for (journalEvent in events) {
            // 数据库靠唯一约束拒绝重复，这里照抄同一条约束，重复写入必须整批失败。
            check(storedEvents.none { storedEvent -> storedEvent.eventIdentifier == journalEvent.eventIdentifier }) {
                "event identifier already stored: ${journalEvent.eventIdentifier.value}"
            }
            check(storedEvents.none { storedEvent -> storedEvent.sequenceNumber == journalEvent.sequenceNumber }) {
                "sequence number already stored: ${journalEvent.sequenceNumber}"
            }
        }

        storedEvents = storedEvents + events
    }

    override suspend fun readEventsAfter(sequenceNumber: Long): List<JournalEvent> =
        storedEvents
            .filter { journalEvent -> journalEvent.sequenceNumber > sequenceNumber }
            .sortedBy { journalEvent -> journalEvent.sequenceNumber }

    override suspend fun latestSequenceNumber(): Long =
        storedEvents.maxOfOrNull { journalEvent -> journalEvent.sequenceNumber }
            ?: EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER

    // 事务回滚用。
    fun snapshot(): List<JournalEvent> = storedEvents

    fun restore(snapshot: List<JournalEvent>) {
        storedEvents = snapshot
    }

    private companion object {
        const val EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER = 0L
    }
}
