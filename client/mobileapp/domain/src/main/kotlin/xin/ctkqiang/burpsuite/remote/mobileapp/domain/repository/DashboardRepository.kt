package xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository

import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.DashboardSummary

/**
 * 主面板的读取端口。
 *
 * 汇总是派生值：连接状态来自远程会话，计数来自各投影，因此这里没有独立的事实来源（plan §44）。
 */
interface DashboardRepository {
    /** 观察主面板汇总；任何一路来源变化都会重新发一次。 */
    fun observeDashboardSummary(): Flow<DashboardSummary>
}
