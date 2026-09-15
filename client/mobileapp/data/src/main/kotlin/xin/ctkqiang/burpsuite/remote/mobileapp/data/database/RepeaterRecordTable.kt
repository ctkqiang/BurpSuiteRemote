// Repeater 表的 DAO。

package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** `repeater` 表的访问接口；与拦截投影同理，重建时需要能清空重写。 */
@Dao
interface RepeaterRecordTable {
    /** 观察全部 Repeater 请求，按创建时刻降序（最新排最前）。 */
    @Query("SELECT * FROM repeater ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RepeaterRecordEntity>>

    /** 观察一条 Repeater 请求；不存在时发 null。 */
    @Query("SELECT * FROM repeater WHERE repeater_request_identifier = :identifier")
    fun observeByIdentifier(identifier: String): Flow<RepeaterRecordEntity?>

    /** 读一条 Repeater 请求；不存在时返回 null。 */
    @Query("SELECT * FROM repeater WHERE repeater_request_identifier = :identifier")
    suspend fun findByIdentifier(identifier: String): RepeaterRecordEntity?

    /** 写一条 Repeater 请求；已存在则覆盖。 */
    @Upsert
    suspend fun upsert(record: RepeaterRecordEntity)

    /** 整批写入；重建时用。 */
    @Upsert
    suspend fun upsertAll(records: List<RepeaterRecordEntity>)

    /** 清空本投影的全部内容；只在重建时调用。 */
    @Query("DELETE FROM repeater")
    suspend fun deleteAll()
}
