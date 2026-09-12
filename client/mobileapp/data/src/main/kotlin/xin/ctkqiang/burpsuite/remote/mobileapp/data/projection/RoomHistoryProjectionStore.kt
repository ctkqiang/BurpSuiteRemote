package xin.ctkqiang.burpsuite.remote.mobileapp.data.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.HistoryRecordTable
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toDomain
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toEntity
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.HistoryProjectionReducer
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.HistoryProjectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionReplayer

/**
 * Room 支撑的历史投影写入口。
 *
 * 增量路径只读出被这条事件牵动的那一行再折，重建路径折整份日志：两条路径共用同一个 reducer，
 * 「重建结果与增量结果一致」才有依据（rules.md §13）。
 */
class RoomHistoryProjectionStore(
    private val historyRecordTable: HistoryRecordTable,
) : ProjectionStore {
    override val projectionName: ProjectionName = ProjectionName.HISTORY

    override fun handles(event: RecordedEvent): Boolean =
        HistoryProjectionReducer.affectedHistoryIdentifier(event) != null

    override suspend fun apply(event: RecordedEvent) {
        val historyIdentifier = HistoryProjectionReducer.affectedHistoryIdentifier(event) ?: return
        val existingRecord = historyRecordTable.findByIdentifier(historyIdentifier.value)?.toDomain()
        val existingState =
            HistoryProjectionState(
                recordsByIdentifier =
                    existingRecord?.let { record -> mapOf(historyIdentifier to record) } ?: emptyMap(),
            )
        val reducedRecords = HistoryProjectionReducer.reduce(existingState, event).recordsByIdentifier
        val updatedRecord = reducedRecords[historyIdentifier] ?: return

        historyRecordTable.upsert(updatedRecord.toEntity())
    }

    override suspend fun replaceAll(events: List<RecordedEvent>) {
        val replayedState = ProjectionReplayer(HistoryProjectionReducer, HistoryProjectionState()).replay(events)

        historyRecordTable.deleteAll()
        historyRecordTable.upsertAll(replayedState.recordsByIdentifier.values.map { record -> record.toEntity() })
    }
}
