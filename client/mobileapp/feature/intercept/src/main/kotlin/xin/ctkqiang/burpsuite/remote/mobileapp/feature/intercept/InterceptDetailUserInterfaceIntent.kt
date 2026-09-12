package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

/** 用户在拦截项详情上的意图。界面只表达「改了什么」，命令构造不在这里（rules.md §8.3）。 */
sealed interface InterceptDetailUserInterfaceIntent {
    /** 用户改了请求行草稿。 */
    data class UpdateRequestLineInput(val requestLineInput: String) : InterceptDetailUserInterfaceIntent

    /** 用户改了请求头草稿。 */
    data class UpdateRequestHeadersInput(val requestHeadersInput: String) : InterceptDetailUserInterfaceIntent
}
