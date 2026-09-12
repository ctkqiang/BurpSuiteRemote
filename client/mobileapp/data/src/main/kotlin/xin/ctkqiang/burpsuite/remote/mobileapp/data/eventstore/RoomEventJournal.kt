package xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore

import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.RemoteEventJournalTable
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toEntity
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toJournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.EventJournal
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider

/**
 * Room 支撑的只追加事件日志。
 *
 * 领域端口只有追加与读取，这里也就只有这两条路径：没有更新、没有删除（rules.md §5.4）。
 */
class RoomEventJournal(
    private val remoteEventJournalTable: RemoteEventJournalTable,
    private val timeProvider: TimeProvider,
) : EventJournal {
    override suspend fun appendEvents(events: List<JournalEvent>) {
        val receivedAt = timeProvider.now()
        remoteEventJournalTable.insert(events.map { journalEvent -> journalEvent.toEntity(receivedAt) })
    }

    override suspend fun readEventsAfter(sequenceNumber: Long): List<JournalEvent> =
        remoteEventJournalTable.readAfter(sequenceNumber).map { entity -> entity.toJournalEvent() }

    override suspend fun latestSequenceNumber(): Long =
        remoteEventJournalTable.latestSequenceNumber() ?: EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER

    private companion object {
        // 与领域侧口径一致：空日志的最新序号是 0，于是「从 0 续传」永远成立。
        const val EMPTY_JOURNAL_LATEST_SEQUENCE_NUMBER = 0L
    }
}
