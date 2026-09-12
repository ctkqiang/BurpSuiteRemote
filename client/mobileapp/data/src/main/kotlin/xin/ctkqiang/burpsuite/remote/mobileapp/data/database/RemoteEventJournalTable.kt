package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** `remote_event_journal` 表的访问接口；只有写入与读取，没有更新与删除（rules.md §5.4）。 */
@Dao
interface RemoteEventJournalTable {
    /** 追加事件；身份或序号与已有行冲突时整批插入失败，由调用方按重复投递处理。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(events: List<RemoteEventJournalEntity>)

    /** 读取某个序号之后的事件，按序号升序。 */
    @Query("SELECT * FROM remote_event_journal WHERE sequence_number > :sequenceNumber ORDER BY sequence_number ASC")
    suspend fun readAfter(sequenceNumber: Long): List<RemoteEventJournalEntity>

    /** 日志中的最大序号；日志为空时返回 null。 */
    @Query("SELECT MAX(sequence_number) FROM remote_event_journal")
    suspend fun latestSequenceNumber(): Long?
}
