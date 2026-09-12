package xin.ctkqiang.burpsuite.remote.mobileapp.data.transaction

import androidx.room.withTransaction
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.BurpRemoteDatabase

/** Room 事务实现；借 Room 的挂起事务把块内的 DAO 调用收进同一个事务。 */
class RoomAtomicUnitOfWork(private val database: BurpRemoteDatabase) : AtomicUnitOfWork {
    override suspend fun <T> runAtomically(block: suspend () -> T): T = database.withTransaction { block() }
}
