package xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore

import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.ProjectionCheckpointTable
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toEntity
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.toProjectionCheckpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.ProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionCheckpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName

/** Room 支撑的校验点存取；一个投影的进度不会替另一个投影说话（rules.md §5.6）。 */
class RoomProjectionCheckpointStore(
    private val projectionCheckpointTable: ProjectionCheckpointTable,
) : ProjectionCheckpointStore {
    override suspend fun readCheckpoint(projectionName: ProjectionName): ProjectionCheckpoint? =
        projectionCheckpointTable.find(projectionName.value)?.toProjectionCheckpoint()

    override suspend fun saveCheckpoint(checkpoint: ProjectionCheckpoint) {
        projectionCheckpointTable.upsert(checkpoint.toEntity())
    }

    override suspend fun readAllCheckpoints(): List<ProjectionCheckpoint> =
        projectionCheckpointTable.readAll().map { entity -> entity.toProjectionCheckpoint() }
}
