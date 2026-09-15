package xin.ctkqiang.burpsuite.remote.mobileapp

/**
 * 应用对外启动意图的额外参数键与系统动作常量。
 *
 * 抽到独立文件而不是塞进 [MainActivity] 的 companion：
 * - [BurpRemoteAppWidgetProvider] 与 [shortcuts.xml] 都要复用 [EXTRA_START_ROUTE]，
 *   跨文件引用一个公开常量比复制字面量更不容易在升级时漂移。
 * - [ACTION_OPEN_ROUTE] 是本应用私有动作名，跟 [android.content.Intent.ACTION_MAIN] 一起用：
 *   MainActivity 既能被启动器 ICON 触发（仅 ACTION_MAIN），也能被静态快捷入口与桌面小部件触发
 *   （ACTION_MAIN + ACTION_OPEN_ROUTE 都在 intent-filter 里匹配上）。
 *
 * 安全说明：[EXTRA_START_ROUTE] 只用于「指定落地屏」。值要进 [SUPPORTED_SHORTCUT_ROUTES]
 * 白名单才被采用，且白名单只含无参数的一级与子路由，不暴露任何敏感动作。
 */
object BurpRemoteIntents {
    /** 落地路由额外参数的键。值是一段路由字符串，进 [SUPPORTED_SHORTCUT_ROUTES] 才被采用。 */
    const val EXTRA_START_ROUTE: String = "startRoute"

    /**
     * 落地路由白名单。与 [BurpsuiteRemoteNavigationHost.SUPPORTED_START_ROUTES] 同源；
     * 这里复制一份是因为导航壳那份是 private，把它暴露出去会让任何外部模块都能拼出带参数的路由。
     */
    val SUPPORTED_SHORTCUT_ROUTES: Set<String> =
        setOf(
            "dashboard",
            "live/history",
            "live/intercept",
            "live/repeater",
            "settings/burp-connection",
            "settings/appearance",
            "settings/language",
            "settings/security",
            "settings/storage",
            "settings/about",
        )
}
