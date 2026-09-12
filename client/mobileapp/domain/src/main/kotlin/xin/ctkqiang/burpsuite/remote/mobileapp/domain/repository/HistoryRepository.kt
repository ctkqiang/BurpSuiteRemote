package xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository

import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord

/**
 * 历史记录的读取端口。
 *
 * 只读：历史记录的事实全部来自事件，界面改不了它（rules.md §5.1）。
 */
interface HistoryRepository {
    /** 观察全部历史记录，按事件序号升序。 */
    fun observeHistoryRecords(): Flow<List<HistoryRecord>>

    /** 观察一条历史记录；不存在时发 null。 */
    fun observeHistoryRecord(historyIdentifier: HistoryIdentifier): Flow<HistoryRecord?>

    /** 读一条历史记录；不存在时返回 null。 */
    suspend fun readHistoryRecord(historyIdentifier: HistoryIdentifier): HistoryRecord?
}
