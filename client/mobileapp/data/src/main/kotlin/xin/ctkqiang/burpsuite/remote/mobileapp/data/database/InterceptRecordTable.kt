package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** `intercept` 表的访问接口；与历史投影同理，重建时需要能清空重写。 */
@Dao
interface InterceptRecordTable {
    /** 观察全部拦截项，按事件序号升序。 */
    @Query("SELECT * FROM intercept ORDER BY sequence_number ASC")
    fun observeAll(): Flow<List<InterceptRecordEntity>>

    /** 观察一条拦截项；不存在时发 null。 */
    @Query("SELECT * FROM intercept WHERE intercept_identifier = :interceptIdentifier")
    fun observeByIdentifier(interceptIdentifier: String): Flow<InterceptRecordEntity?>

    /** 读一条拦截项；不存在时返回 null。 */
    @Query("SELECT * FROM intercept WHERE intercept_identifier = :interceptIdentifier")
    suspend fun findByIdentifier(interceptIdentifier: String): InterceptRecordEntity?

    /** 写一条拦截项；已存在则覆盖。 */
    @Upsert
    suspend fun upsert(record: InterceptRecordEntity)

    /** 整批写入；重建时用。 */
    @Upsert
    suspend fun upsertAll(records: List<InterceptRecordEntity>)

    /** 清空本投影的全部内容；只在重建时调用。 */
    @Query("DELETE FROM intercept")
    suspend fun deleteAll()
}
