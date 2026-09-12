package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

/**
 * 用户在主面板上的意图。
 *
 * 界面不自己跳转：跳转是一件事，由 ViewModel 发一次性效果交给装配层（rules.md §8.1）。
 */
sealed interface DashboardUserInterfaceIntent {
    /** 打开实时历史。 */
    data object OpenLiveHistory : DashboardUserInterfaceIntent

    /** 打开拦截队列。 */
    data object OpenLiveIntercept : DashboardUserInterfaceIntent

    /** 打开重放。 */
    data object OpenLiveRepeater : DashboardUserInterfaceIntent

    /** 打开 Recent 列表里某一条的详情。 */
    data class OpenHistoryRecord(val historyIdentifier: String) : DashboardUserInterfaceIntent
}
