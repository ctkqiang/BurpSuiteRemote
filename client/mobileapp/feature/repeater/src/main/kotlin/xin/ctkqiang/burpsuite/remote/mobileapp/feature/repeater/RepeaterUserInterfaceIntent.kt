

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

/**
 * 用户在重放屏上的意图。
 *
 * 这一屏暂时只支持刷新连接状态：执行与新建都要发控制命令，客户端还没有那条通路。
 */
sealed interface RepeaterUserInterfaceIntent {
    /** 用户下拉刷新。 */
    data object Refresh : RepeaterUserInterfaceIntent
}
