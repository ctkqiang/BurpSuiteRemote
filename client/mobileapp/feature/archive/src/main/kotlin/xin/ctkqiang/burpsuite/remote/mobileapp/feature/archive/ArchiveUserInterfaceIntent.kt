

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

/** 用户在归档屏上的意图。界面只表达「想看哪个 / 对哪条做什么」，跳转走一次性效果（rules.md §8.1）。 */
sealed interface ArchiveUserInterfaceIntent {
    /** 用户下拉刷新。 */
    data object Refresh : ArchiveUserInterfaceIntent

    /** 切换页签。 */
    data class SelectTab(val tab: ArchiveTab) : ArchiveUserInterfaceIntent

    /** 打开某条已保存记录的详情。 */
    data class OpenSavedRecord(val historyIdentifier: String) : ArchiveUserInterfaceIntent

    /** 分享或导出某条已保存记录。 */
    data class ShareSavedRecord(val historyIdentifier: String) : ArchiveUserInterfaceIntent

    /** 打开截图页。 */
    data object OpenScreenshots : ArchiveUserInterfaceIntent
}
