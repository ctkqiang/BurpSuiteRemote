package xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent

/**
 * 事件日志的端口。
 *
 * 只有追加与读取：日志是事实的唯一落点，没有任何更新或删除路径（rules.md §5.4）。
 * 追加必须由「把事件与各投影校验点放在同一个事务里」的调用方发起，否则会留下事件在、投影没跟上的偏差。
 */
interface EventJournal {
    /** 追加事件；身份或序号重复时整批不落，由调用方按重复投递处理。 */
    suspend fun appendEvents(events: List<JournalEvent>)

    /** 读取某个序号之后的事件，按序号升序。 */
    suspend fun readEventsAfter(sequenceNumber: Long): List<JournalEvent>

    /** 日志中已有的最大序号；日志为空时返回 0。 */
    suspend fun latestSequenceNumber(): Long
}
