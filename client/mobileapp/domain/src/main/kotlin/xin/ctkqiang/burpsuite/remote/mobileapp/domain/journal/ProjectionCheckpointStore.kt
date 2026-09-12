package xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionCheckpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName

/**
 * 各投影校验点的存取端口。
 *
 * 校验点按投影名分开存放，一个投影的进度既不能代表另一个投影，也不能拿来当事实（rules.md §5.6）。
 */
interface ProjectionCheckpointStore {
    /** 读取某个投影的校验点；还没折过任何事件时返回 null。 */
    suspend fun readCheckpoint(projectionName: ProjectionName): ProjectionCheckpoint?

    /** 写入校验点；同名覆盖。 */
    suspend fun saveCheckpoint(checkpoint: ProjectionCheckpoint)

    /** 读取全部校验点，按投影名升序。 */
    suspend fun readAllCheckpoints(): List<ProjectionCheckpoint>
}
