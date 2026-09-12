package xin.ctkqiang.burpsuite.remote.mobileapp.data.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.InterceptRecordTable
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toDomain
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toEntity
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.InterceptProjectionReducer
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.InterceptProjectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionReplayer

/** Room 支撑的拦截投影写入口；增量只折被牵动的那一行，重建折整份日志，共用同一个 reducer。 */
class RoomInterceptProjectionStore(
    private val interceptRecordTable: InterceptRecordTable,
) : ProjectionStore {
    override val projectionName: ProjectionName = ProjectionName.INTERCEPT

    override fun handles(event: RecordedEvent): Boolean =
        InterceptProjectionReducer.affectedInterceptIdentifier(event) != null

    override suspend fun apply(event: RecordedEvent) {
        val interceptIdentifier = InterceptProjectionReducer.affectedInterceptIdentifier(event) ?: return
        val existingRecord = interceptRecordTable.findByIdentifier(interceptIdentifier.value)?.toDomain()
        val existingState =
            InterceptProjectionState(
                recordsByIdentifier =
                    existingRecord?.let { record -> mapOf(interceptIdentifier to record) } ?: emptyMap(),
            )
        val reducedRecords = InterceptProjectionReducer.reduce(existingState, event).recordsByIdentifier
        val updatedRecord = reducedRecords[interceptIdentifier] ?: return

        interceptRecordTable.upsert(updatedRecord.toEntity())
    }

    override suspend fun replaceAll(events: List<RecordedEvent>) {
        val replayedState = ProjectionReplayer(InterceptProjectionReducer, InterceptProjectionState()).replay(events)

        interceptRecordTable.deleteAll()
        interceptRecordTable.upsertAll(replayedState.recordsByIdentifier.values.map { record -> record.toEntity() })
    }
}
