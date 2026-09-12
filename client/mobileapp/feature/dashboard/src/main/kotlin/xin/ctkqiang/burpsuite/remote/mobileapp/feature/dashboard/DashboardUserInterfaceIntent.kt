

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

    /** 用户从顶部动作里打开了扫码配对。 */
    data object OpenPairingScanner : DashboardUserInterfaceIntent

    /** 打开实时历史。 */
    data object OpenLiveHistory : DashboardUserInterfaceIntent

    /** 打开拦截队列。 */
    data object OpenLiveIntercept : DashboardUserInterfaceIntent

    /** 打开重放。 */
    data object OpenLiveRepeater : DashboardUserInterfaceIntent

    /** 打开 Recent 列表里某一条的详情。 */
    data class OpenHistoryRecord(val historyIdentifier: String) : DashboardUserInterfaceIntent
}
