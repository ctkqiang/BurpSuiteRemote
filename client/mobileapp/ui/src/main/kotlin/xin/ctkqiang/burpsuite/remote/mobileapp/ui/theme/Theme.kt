package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/**
 * 应用主题。界面里任何颜色、字体都从这里取，不自己写色值。
 *
 * 没开动态取色：动态色会把品牌橙换成壁纸配色，品牌色就没了。
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

    MaterialTheme(
        colorScheme = if (useDarkColours) DarkColours else LightColours,
        content = content,
    )
}
