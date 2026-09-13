package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import android.net.Uri

/**
 * 导航图里的路由名（plan §43）。
 *
 * 只存字符串：界面模块拿不到 NavController，建 NavHost 是装配层的事（rules.md §6.1）。
 * 带标识的屏用「模板 + 构造函数」两个成员：模板给 NavHost 注册，构造函数给跳转方用，
 * 这样标识里的斜杠、百分号之类不会把路径拆散。
 */
object BurpRemoteRoute {
    /** 主面板。 */
    const val DASHBOARD: String = "dashboard"

    /** 实时区索引。 */
    const val LIVE_SECTION: String = "live"

    /** 实时历史列表。 */
    const val LIVE_HISTORY: String = "live/history"

    /** 路径参数名：历史记录标识。 */
    const val HISTORY_IDENTIFIER_ARGUMENT: String = "historyIdentifier"

    /** 历史详情。 */
    const val LIVE_HISTORY_DETAIL: String = "live/history/detail/{$HISTORY_IDENTIFIER_ARGUMENT}"

    /** 拦截队列。 */
    const val LIVE_INTERCEPT: String = "live/intercept"

    /** 路径参数名：拦截项标识。 */
    const val INTERCEPT_IDENTIFIER_ARGUMENT: String = "interceptIdentifier"

    /** 拦截项详情与编辑。 */
    const val LIVE_INTERCEPT_DETAIL: String = "live/intercept/detail/{$INTERCEPT_IDENTIFIER_ARGUMENT}"

    /** 重放。 */
    const val LIVE_REPEATER: String = "live/repeater"

    /** 分享与导出；带一条历史记录作导出对象。 */
    const val SHARING: String = "sharing/{$HISTORY_IDENTIFIER_ARGUMENT}"

    /** 设置区索引。 */
    const val SETTINGS_SECTION: String = "settings"

    /** Burp 连接。 */
    const val SETTINGS_BURP_CONNECTION: String = "settings/burp-connection"

    /** 外观（主题）。 */
    const val SETTINGS_APPEARANCE: String = "settings/appearance"

    /** 语言。 */
    const val SETTINGS_LANGUAGE: String = "settings/language"

    /** 安全。 */
    const val SETTINGS_SECURITY: String = "settings/security"

    /** 存储。 */
    const val SETTINGS_STORAGE: String = "settings/storage"

    /** 某条历史记录的详情路由。 */
    fun liveHistoryDetail(historyIdentifier: String): String = "live/history/detail/${Uri.encode(historyIdentifier)}"

    /** 某个拦截项的详情路由。 */
    fun liveInterceptDetail(interceptIdentifier: String): String =
        "live/intercept/detail/" + Uri.encode(interceptIdentifier)

    /** 某条历史记录的分享与导出路由。 */
    fun sharing(historyIdentifier: String): String = "sharing/${Uri.encode(historyIdentifier)}"
}
