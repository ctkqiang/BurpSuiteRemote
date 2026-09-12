package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

/** 用户在历史详情上的意图。界面只表达「想干什么」，导航与命令构造都不在这里（rules.md §8.1）。 */
sealed interface HistoryDetailUserInterfaceIntent {
    /** 分享或导出这条记录。 */
    data object ShareHistoryRecord : HistoryDetailUserInterfaceIntent
}
