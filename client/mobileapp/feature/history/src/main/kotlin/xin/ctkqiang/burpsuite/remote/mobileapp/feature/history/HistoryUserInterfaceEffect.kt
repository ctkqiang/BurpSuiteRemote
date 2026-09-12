

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

/** 实时历史列表发出的一次性效果；不进状态，免得转屏后重放一次跳转（rules.md §8.1）。 */
sealed interface HistoryUserInterfaceEffect {
    /** 请求装配层导航到某条记录的详情。 */
    data class OpenHistoryRecord(val historyIdentifier: String) : HistoryUserInterfaceEffect
}
