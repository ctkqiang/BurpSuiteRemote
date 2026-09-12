package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

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
)
