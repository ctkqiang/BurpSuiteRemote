package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState

/**
 * 重放屏状态。
 *
 * 客户端既没有重放请求的读端口，也没有执行命令的端口，因此这里只有连接状态是真的：
 * 列表与执行结果都只能显示「还没有投影」，不编造条目（rules.md §5.1）。
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
) {
    /** 会话此刻是否真的可用；只有 Connected 算在线（plan §59）。 */
    val isLive: Boolean get() = connectionState.isConnected
}
