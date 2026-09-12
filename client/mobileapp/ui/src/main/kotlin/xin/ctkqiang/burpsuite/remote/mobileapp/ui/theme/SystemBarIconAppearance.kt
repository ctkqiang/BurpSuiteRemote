package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

/**
 * 状态栏与导航栏图标的明暗。
 *
 * 抽成可断言的值而不是直接写进界面：这条映射错了的表现是「状态栏的字消失」，而它只在真机上
 * 看得见。一个纯函数加一条断言，比装到机器上再看一眼可靠（rules.md §13）。
 *
 * @property isLightStatusBar 状态栏是否改用深色图标（也就是界面里说的「浅色状态栏」）。
 * @property isLightNavigationBar 导航栏是否改用深色图标。
 */
data class SystemBarIconAppearance(
    val isLightStatusBar: Boolean,
    val isLightNavigationBar: Boolean,
) {
    companion object {
        /** 深色主题配浅色图标、浅色主题配深色图标；两根系统栏始终同向，否则总有一根看不清。 */
        fun of(isDarkTheme: Boolean): SystemBarIconAppearance =
            SystemBarIconAppearance(
                isLightStatusBar = !isDarkTheme,
                isLightNavigationBar = !isDarkTheme,
            )
    }
}
