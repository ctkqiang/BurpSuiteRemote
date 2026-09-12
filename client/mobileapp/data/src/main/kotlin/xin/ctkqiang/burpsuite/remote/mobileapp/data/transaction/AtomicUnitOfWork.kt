package xin.ctkqiang.burpsuite.remote.mobileapp.data.transaction

/**
 * 事务边界。
 *
 * 「事件与投影校验点要么都成、要么都不成」这条约束需要一个能圈住多个写操作的事务，
 * 但领域层不许认识 Room，因此把边界抽成一个端口，由适配层决定它背后是什么（rules.md §5.4）。
 */
interface AtomicUnitOfWork {
    /** 原子执行 [block]；抛出即整体回滚，调用方接到的还是那个异常。 */
    suspend fun <T> runAtomically(block: suspend () -> T): T
}
