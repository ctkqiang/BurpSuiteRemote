// 客户端本地库。

package xin.ctkqiang.burpsuite.remote.mobileapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 客户端本地库。
 *
 * 只收当前真有消费方的表：事件日志与投影校验点是事实层，`history`、`intercept` 与 `repeater` 是读模型。
 * plan §18 里其余的表还没有端口在读写，等出现消费方再建，免得留下永远空着的表。
 */
@Database(
    entities = [
        RemoteEventJournalEntity::class,
        ProjectionCheckpointEntity::class,
        HistoryRecordEntity::class,
        InterceptRecordEntity::class,
        RepeaterRecordEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class BurpRemoteDatabase : RoomDatabase() {
    abstract fun remoteEventJournalTable(): RemoteEventJournalTable

    abstract fun projectionCheckpointTable(): ProjectionCheckpointTable

    abstract fun historyRecordTable(): HistoryRecordTable

    abstract fun interceptRecordTable(): InterceptRecordTable

    abstract fun repeaterRecordTable(): RepeaterRecordTable
}
