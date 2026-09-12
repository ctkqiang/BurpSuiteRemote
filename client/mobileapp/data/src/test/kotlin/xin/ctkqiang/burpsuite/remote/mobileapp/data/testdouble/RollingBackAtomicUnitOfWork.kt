package xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble

import xin.ctkqiang.burpsuite.remote.mobileapp.data.transaction.AtomicUnitOfWork

// 快照/回滚式事务执行器：用它替代 Room 的事务，把「要么都成、要么都不成」搬进纯 JVM 用例。
internal class RollingBackAtomicUnitOfWork(
    private val eventJournal: InMemoryEventJournal,
    private val projectionCheckpointStore: InMemoryProjectionCheckpointStore,
) : AtomicUnitOfWork {
    override suspend fun <T> runAtomically(block: suspend () -> T): T {
        val journalSnapshot = eventJournal.snapshot()
        val checkpointSnapshot = projectionCheckpointStore.snapshot()

        return try {
            block()
        } catch (failure: Throwable) {
            eventJournal.restore(journalSnapshot)
            projectionCheckpointStore.restore(checkpointSnapshot)

            throw failure
        }
    }
}
