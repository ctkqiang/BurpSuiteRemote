package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

/** 拦截队列发出的一次性效果；不进状态，免得转屏后重放一次跳转（rules.md §8.1）。 */
sealed interface InterceptUserInterfaceEffect {
    /** 请求装配层导航到某条拦截项的详情。 */
    data class OpenInterceptRecord(val interceptIdentifier: String) : InterceptUserInterfaceEffect
}
