package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

/** 历史详情发出的一次性效果；不进状态，免得转屏后重放一次跳转（rules.md §8.1）。 */
sealed interface HistoryDetailUserInterfaceEffect {
    /** 请求装配层导航到分享与导出。 */
    data class OpenSharing(val historyIdentifier: String) : HistoryDetailUserInterfaceEffect
}
