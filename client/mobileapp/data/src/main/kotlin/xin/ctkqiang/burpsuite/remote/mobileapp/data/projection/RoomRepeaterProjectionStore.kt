// Room 支撑的 Repeater 投影写入口。

package xin.ctkqiang.burpsuite.remote.mobileapp.data.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.RepeaterRecordTable
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toDomain
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toEntity
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionReplayer
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.RepeaterProjectionReducer
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.RepeaterProjectionState

/** Room 支撑的 Repeater 投影写入口；增量只折被牵动的那一行，重建折整份日志，共用同一个 reducer。 */
class RoomRepeaterProjectionStore(
    private val repeaterRecordTable: RepeaterRecordTable,
) : ProjectionStore {
    override val projectionName: ProjectionName = ProjectionName.REPEATER

    override fun handles(event: RecordedEvent): Boolean =
        RepeaterProjectionReducer.affectedRepeaterIdentifier(event) != null

    override suspend fun apply(event: RecordedEvent) {
        val identifier = RepeaterProjectionReducer.affectedRepeaterIdentifier(event) ?: return
        val existingRecord = repeaterRecordTable.findByIdentifier(identifier.value)?.toDomain()
        val existingState =
            RepeaterProjectionState(
                recordsByIdentifier =
                    existingRecord?.let { record -> mapOf(identifier to record) } ?: emptyMap(),
            )
        val reducedRecords = RepeaterProjectionReducer.reduce(existingState, event).recordsByIdentifier
        val updatedRecord = reducedRecords[identifier] ?: return

        repeaterRecordTable.upsert(updatedRecord.toEntity())
    }

    override suspend fun replaceAll(events: List<RecordedEvent>) {
        val replayedState =
            ProjectionReplayer(RepeaterProjectionReducer, RepeaterProjectionState()).replay(events)

        repeaterRecordTable.deleteAll()
        repeaterRecordTable.upsertAll(
            replayedState.recordsByIdentifier.values.map { record -> record.toEntity() },
        )
    }
}
