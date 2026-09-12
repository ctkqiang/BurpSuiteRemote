package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord

/**
 * 主面板状态（plan §44）。
 *
 * 每个字段都是只读展示值：计数来自投影，最近列表来自历史投影，界面不再自己算一遍（plan §42）。
 */
data class DashboardUserInterfaceState(
    /** 当前连接状态。 */
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    /** 最近一条历史记录的目标主机；还没有任何记录时为空，界面按「不知道」显示。 */
    val targetHost: String? = null,
    /** 仍在实时投影中的请求数。 */
    val liveRequestCount: Int = 0,
    /** 还停在拦截队列里的请求数。 */
    val interceptedCount: Int = 0,
    /** 已归档的请求数。 */
    val savedCount: Int = 0,
    /** Recent 列表；按时间倒序。 */
    val recentRecords: List<HistoryRecord> = emptyList(),
)
