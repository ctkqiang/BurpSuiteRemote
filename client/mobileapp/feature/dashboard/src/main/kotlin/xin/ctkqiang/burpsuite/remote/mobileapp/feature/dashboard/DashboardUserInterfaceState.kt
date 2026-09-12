package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import java.time.Instant

/**
 * 主面板状态（plan §44）。
 *
 * 「还没读出来」「读失败」「读出来是空的」是三件不同的事，因此分别表达：界面照它决定画等待、
 * 画可重试的错误，还是画数据，而不是拿一个空列表冒充「没有请求」（rules.md §5.1）。
 */
data class DashboardUserInterfaceState(
    /** 当前连接状态；LIVE 与否只看它。 */
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    /** 最近一条历史记录的目标主机；还没有任何记录时为 null，界面按「不知道」显示。 */
    val targetHost: String? = null,
    /** 仍在实时投影中的请求数。 */
    val liveRequestCount: Int = 0,
    /** 还停在拦截队列里的请求数。 */
    val interceptedCount: Int = 0,
    /** 已归档的请求数。 */
    val savedCount: Int = 0,
    /** 最近观测到的请求，按时间倒序。 */
    val recentRecords: List<HistoryRecord> = emptyList(),
    /** 相对时间的参照点；由 ViewModel 注入的时钟给出，界面不自己读系统时钟。 */
    val now: Instant = Instant.EPOCH,
    /** 是否已经从仓库读到过一次结果。 */
    val hasLoaded: Boolean = false,
    /** 本次刷新是否还在进行；下拉指示器只看它。 */
    val isRefreshing: Boolean = false,
    /** 读取失败的原因；成功时为 null。 */
    @StringRes val failureReasonResource: Int? = null,
) {
    /** 会话此刻是否真的可用；只有 Connected 算在线（plan §59）。 */
    val isLive: Boolean get() = connectionState.isConnected
}
