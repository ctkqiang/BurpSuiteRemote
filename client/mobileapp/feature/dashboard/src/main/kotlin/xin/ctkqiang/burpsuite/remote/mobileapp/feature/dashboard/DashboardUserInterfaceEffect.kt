

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

/** 主面板发出的一次性效果；不进状态，免得转屏后重放一次跳转（rules.md §8.1）。 */
sealed interface DashboardUserInterfaceEffect {
    /** 请求装配层打开扫码配对（也就是连接屏的扫码面板）。 */
    data object OpenPairingScanner : DashboardUserInterfaceEffect

    /** 请求装配层导航到实时历史。 */
    data object OpenLiveHistory : DashboardUserInterfaceEffect

    /** 请求装配层导航到拦截队列。 */
    data object OpenLiveIntercept : DashboardUserInterfaceEffect

    /** 请求装配层导航到重放。 */
    data object OpenLiveRepeater : DashboardUserInterfaceEffect

    /** 请求装配层导航到某条历史记录的详情。 */
    data class OpenHistoryRecord(val historyIdentifier: String) : DashboardUserInterfaceEffect
}
