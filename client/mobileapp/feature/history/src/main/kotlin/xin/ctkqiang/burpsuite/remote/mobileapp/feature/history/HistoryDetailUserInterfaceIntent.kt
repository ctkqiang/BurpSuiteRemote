package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

/** 用户在历史详情上的意图。界面只表达「想干什么」，导航与命令构造都不在这里（rules.md §8.1）。 */
sealed interface HistoryDetailUserInterfaceIntent {
    /** 分享或导出这条记录。 */
    data object ShareHistoryRecord : HistoryDetailUserInterfaceIntent

    /** 重新取一次报文本体；上一次失败后用户能自己再试。 */
    data object ReloadHistoryMessage : HistoryDetailUserInterfaceIntent

    /** 把这条记录的主机加入 Burp 作用域；加过之后用户还能再点，插件的写入本身是幂等的。 */
    data object AddHistoryHostToScope : HistoryDetailUserInterfaceIntent
}
