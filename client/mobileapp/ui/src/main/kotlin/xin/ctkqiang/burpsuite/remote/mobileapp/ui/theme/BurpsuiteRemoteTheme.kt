package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/**
 * 应用主题。界面里任何颜色、字体都从这里取，不自己写色值。
 *
 * 不接动态取色：动态色会把品牌橙换成壁纸配色，品牌标识就没了（rules.md §10）。
 */
@Composable
fun BurpsuiteRemoteTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDarkColours =
        when (themeMode) {
            ThemeMode.Automatic -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

    val tokens =
        remember(useDarkColours) {
            BurpRemoteDesignTokens(
                colourScheme = if (useDarkColours) BurpRemoteDarkColourScheme else BurpRemoteLightColourScheme,
                typography = BurpRemoteTypographyTokens,
                isDark = useDarkColours,
            )
        }

    CompositionLocalProvider(LocalBurpRemoteDesignTokens provides tokens, content = content)
}
