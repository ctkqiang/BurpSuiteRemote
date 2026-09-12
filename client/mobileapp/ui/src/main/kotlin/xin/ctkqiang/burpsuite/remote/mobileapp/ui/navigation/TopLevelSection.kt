package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R

/**
 * 底栏的一级分区（plan §43）。
 *
 * [indexEntries] 就是分区索引屏的内容——分区里每个子屏都在这里出现一次，因此「界面能到哪些页」
 * 只看这一张表，不用翻 NavHost。
 */
enum class TopLevelSection(
    /** 底栏显示的分区名。 */
    @StringRes val labelResource: Int,
    /** 点这个分区落在哪个路由。 */
    val landingRoute: String,
    /** 底栏图标。 */
    val icon: ImageVector,
    /** 索引屏的子屏；主面板是终点屏，因此为空表。 */
    val indexEntries: List<SectionMenuEntry>,
) {
    /** 主面板。 */
    Dashboard(
        labelResource = R.string.navigation_section_dashboard,
        landingRoute = BurpRemoteRoute.DASHBOARD,
        icon = Icons.Filled.Home,
        indexEntries = emptyList(),
    ),

    /** 实时区：历史、拦截、重放。 */
    Live(
        labelResource = R.string.navigation_section_live,
        landingRoute = BurpRemoteRoute.LIVE_SECTION,
        icon = Icons.AutoMirrored.Filled.List,
        indexEntries =
            listOf(
                SectionMenuEntry(
                    labelResource = R.string.navigation_entry_live_history,
                    route = BurpRemoteRoute.LIVE_HISTORY,
                ),
                SectionMenuEntry(
                    labelResource = R.string.navigation_entry_live_intercept,
                    route = BurpRemoteRoute.LIVE_INTERCEPT,
                ),
                SectionMenuEntry(
                    labelResource = R.string.navigation_entry_live_repeater,
                    route = BurpRemoteRoute.LIVE_REPEATER,
                ),
            ),
    ),

    /** 归档区：入口就是三个页签（已保存历史、书签、截图），因此没有索引页。 */
    Archive(
        labelResource = R.string.navigation_section_archive,
        landingRoute = BurpRemoteRoute.ARCHIVE_SECTION,
        icon = Icons.Filled.DateRange,
        indexEntries = emptyList(),
    ),

    /** 设置区：连接、外观、语言、安全、存储。 */
    Settings(
        labelResource = R.string.navigation_section_settings,
        landingRoute = BurpRemoteRoute.SETTINGS_SECTION,
        icon = Icons.Filled.Settings,
        indexEntries =
            listOf(
                SectionMenuEntry(
                    labelResource = R.string.navigation_entry_settings_burp_connection,
                    route = BurpRemoteRoute.SETTINGS_BURP_CONNECTION,
                ),
                SectionMenuEntry(
                    labelResource = R.string.navigation_entry_settings_appearance,
                    route = BurpRemoteRoute.SETTINGS_APPEARANCE,
                ),
                SectionMenuEntry(
                    labelResource = R.string.navigation_entry_settings_language,
                    route = BurpRemoteRoute.SETTINGS_LANGUAGE,
                ),
                SectionMenuEntry(
                    labelResource = R.string.navigation_entry_settings_security,
                    route = BurpRemoteRoute.SETTINGS_SECURITY,
                ),
                SectionMenuEntry(
                    labelResource = R.string.navigation_entry_settings_storage,
                    route = BurpRemoteRoute.SETTINGS_STORAGE,
                ),
            ),
    ),
    ;

    /** 当前路由是否落在本分区；分区索引、子屏与子屏的详情都算。 */
    fun containsRoute(route: String?): Boolean {
        if (route == null) return false
        return route == landingRoute || route.startsWith("$landingRoute/")
    }
}
