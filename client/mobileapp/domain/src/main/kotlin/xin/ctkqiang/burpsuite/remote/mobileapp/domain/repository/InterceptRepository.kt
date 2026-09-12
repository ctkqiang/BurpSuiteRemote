package xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository

import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord

/**
 * 拦截项的读取端口。
 *
 * 只读：放行与丢弃是命令，写路径属于命令侧，不从这里绕过去（rules.md §5.1）。
 */
interface InterceptRepository {
    /** 观察全部拦截项，按事件序号升序。 */
    fun observeInterceptRecords(): Flow<List<InterceptRecord>>

    /** 观察一条拦截项；不存在时发 null。 */
    fun observeInterceptRecord(interceptIdentifier: InterceptIdentifier): Flow<InterceptRecord?>
}
