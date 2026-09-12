package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** `projection_checkpoint` 表的访问接口；校验点按投影名分开存放（rules.md §5.6）。 */
@Dao
interface ProjectionCheckpointTable {
    /** 读某个投影的校验点；还没有折过任何事件时返回 null。 */
    @Query("SELECT * FROM projection_checkpoint WHERE projection_name = :projectionName")
    suspend fun find(projectionName: String): ProjectionCheckpointEntity?

    /** 读全部校验点，按投影名升序。 */
    @Query("SELECT * FROM projection_checkpoint ORDER BY projection_name ASC")
    suspend fun readAll(): List<ProjectionCheckpointEntity>

    /** 写入校验点；同名覆盖。 */
    @Upsert
    suspend fun upsert(checkpoint: ProjectionCheckpointEntity)
}
