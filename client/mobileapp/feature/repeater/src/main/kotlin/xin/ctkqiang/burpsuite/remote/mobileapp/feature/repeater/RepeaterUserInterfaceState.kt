// Repeater 屏状态。

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.RepeaterRecord

/**
 * 重放屏状态。
 *
 * 读端口从 RepeaterRepository 来（事件驱动投影），连接状态从 DashboardRepository 来。
 */
data class RepeaterUserInterfaceState(
    /** 当前连接状态；重放是实时区功能，离线时它本来就不该可用。 */
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    /** 是否已经从仓库读到过一次结果。 */
    val hasLoaded: Boolean = false,
    /** 本次刷新是否还在进行。 */
    val isRefreshing: Boolean = false,
    /** 读取失败的原因；成功时为 null。 */
    @StringRes val failureReasonResource: Int? = null,
    /** 全部 Repeater 请求，按创建时刻降序（最新排最前）。 */
    val records: List<RepeaterRecord> = emptyList(),
) {
    /** 会话此刻是否真的可用；只有 Connected 算在线（plan §59）。 */
    val isLive: Boolean get() = connectionState.isConnected
}
