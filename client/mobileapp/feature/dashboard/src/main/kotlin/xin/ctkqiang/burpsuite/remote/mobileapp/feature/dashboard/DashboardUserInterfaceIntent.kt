package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

/**
 * 用户在主面板上的意图。
 *
 * 界面不自己跳转、也不自己重读仓库：两件事都交给 ViewModel，界面只表达「用户想干什么」
 * （rules.md §8.1）。
 */
sealed interface DashboardUserInterfaceIntent {
    /** 用户下拉刷新。 */
    data object Refresh : DashboardUserInterfaceIntent

    /** 打开最近请求里某一条的详情。 */
    data class OpenHistoryRecord(val historyIdentifier: String) : DashboardUserInterfaceIntent
}
