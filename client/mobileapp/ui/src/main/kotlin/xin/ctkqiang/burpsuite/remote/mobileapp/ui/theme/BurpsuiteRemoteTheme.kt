package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeFlavor
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/**
 * 应用主题。界面里任何颜色、字体都从这里取，不自己写色值。
 *
 * 明暗（[themeMode]）与口味（[themeFlavor]）正交：前者决定底色深浅，后者决定强调色与整体色调。
 * 「跟随系统」时明暗随系统翻转，但口味不变——因此任意口味下系统切换都能正确翻到对应那一份。
 *
 * 不接动态取色：动态色会把品牌橙换成壁纸配色，品牌标识就没了（rules.md §10）。
 */
@Composable
fun BurpsuiteRemoteTheme(
    themeMode: ThemeMode,
    themeFlavor: ThemeFlavor = ThemeFlavor.BurpClassic,
    content: @Composable () -> Unit,
) {
    val useDarkColours =
        when (themeMode) {
            ThemeMode.Automatic -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

    val tokens =
        remember(useDarkColours, themeFlavor) {
            BurpRemoteDesignTokens(
                colourScheme = BurpRemoteFlavourPalettes.schemeFor(themeFlavor, useDarkColours),
                typography = BurpRemoteTypographyTokens,
                isDark = useDarkColours,
            )
        }

    CompositionLocalProvider(LocalBurpRemoteDesignTokens provides tokens, content = content)
}
