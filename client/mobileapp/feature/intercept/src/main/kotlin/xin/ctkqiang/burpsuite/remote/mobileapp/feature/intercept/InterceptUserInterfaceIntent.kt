package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

/** 用户在拦截队列上的意图。界面不自己跳转，跳转走一次性效果（rules.md §8.1）。 */
sealed interface InterceptUserInterfaceIntent {
    /** 打开某一条的详情与编辑。 */
    data class OpenInterceptRecord(val interceptIdentifier: String) : InterceptUserInterfaceIntent
}
