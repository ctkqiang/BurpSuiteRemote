package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** `history` 表的访问接口；投影整份重建时要能清空重写，因此这里允许删除（投影不是事实，rules.md §5.6）。 */
@Dao
interface HistoryRecordTable {
    /** 观察全部历史记录，按事件序号升序。 */
    @Query("SELECT * FROM history ORDER BY sequence_number ASC")
    fun observeAll(): Flow<List<HistoryRecordEntity>>

    /** 观察一条历史记录；不存在时发 null。 */
    @Query("SELECT * FROM history WHERE history_identifier = :historyIdentifier")
    fun observeByIdentifier(historyIdentifier: String): Flow<HistoryRecordEntity?>

    /** 读一条历史记录；不存在时返回 null。 */
    @Query("SELECT * FROM history WHERE history_identifier = :historyIdentifier")
    suspend fun findByIdentifier(historyIdentifier: String): HistoryRecordEntity?

    /** 写一条历史记录；已存在则覆盖。 */
    @Upsert
    suspend fun upsert(record: HistoryRecordEntity)

    /** 整批写入；重建时用。 */
    @Upsert
    suspend fun upsertAll(records: List<HistoryRecordEntity>)

    /** 清空本投影的全部内容；只在重建时调用。 */
    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
