// Repeater 请求的只读端口。

package xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository

import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.RepeaterRecord

/**
 * Repeater 请求的读取端口。
 *
 * 只读：执行命令走独立的命令通路（plan §43 的 execute），不从这里绕过去（rules.md §5.1）。
 */
interface RepeaterRepository {
    /** 观察全部 Repeater 请求，按创建时刻降序（最新的排最前）。 */
    fun observeRepeaterRecords(): Flow<List<RepeaterRecord>>

    /** 观察一条 Repeater 请求；不存在时发 null。 */
    fun observeRepeaterRecord(repeaterRequestIdentifier: RepeaterRequestIdentifier): Flow<RepeaterRecord?>
}
