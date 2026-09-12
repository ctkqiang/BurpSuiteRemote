package xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.ProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionCheckpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName

// 内存校验点存取；带快照与回滚。
internal class InMemoryProjectionCheckpointStore : ProjectionCheckpointStore {
    private var storedCheckpoints: Map<ProjectionName, ProjectionCheckpoint> = emptyMap()

    override suspend fun readCheckpoint(projectionName: ProjectionName): ProjectionCheckpoint? =
        storedCheckpoints[projectionName]

    override suspend fun saveCheckpoint(checkpoint: ProjectionCheckpoint) {
        storedCheckpoints = storedCheckpoints + (checkpoint.projectionName to checkpoint)
    }

    override suspend fun readAllCheckpoints(): List<ProjectionCheckpoint> =
        storedCheckpoints.values.sortedBy { checkpoint -> checkpoint.projectionName.value }

    fun snapshot(): Map<ProjectionName, ProjectionCheckpoint> = storedCheckpoints

    fun restore(snapshot: Map<ProjectionName, ProjectionCheckpoint>) {
        storedCheckpoints = snapshot
    }
}
