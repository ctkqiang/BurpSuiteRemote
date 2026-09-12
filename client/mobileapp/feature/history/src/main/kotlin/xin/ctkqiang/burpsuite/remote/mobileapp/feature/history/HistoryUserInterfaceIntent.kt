

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

/**
 * 用户在实时历史列表上的意图。
 *
 * 界面不自己跳转、也不自己重读仓库：两件事都交给 ViewModel（rules.md §8.1）。
 */
sealed interface HistoryUserInterfaceIntent {
    /** 用户下拉刷新。 */
    data object Refresh : HistoryUserInterfaceIntent

    /** 打开某条记录的详情。 */
    data class OpenHistoryRecord(val historyIdentifier: String) : HistoryUserInterfaceIntent
}
