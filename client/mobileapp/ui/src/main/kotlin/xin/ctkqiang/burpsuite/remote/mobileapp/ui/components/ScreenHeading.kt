package xin.ctkqiang.burpsuite.remote.mobileapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/** 屏幕标题。各屏统一用它，免得同一层级的标题在不同屏上字号不一。 */
@Composable
fun ScreenHeading(
    @StringRes titleResource: Int,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    BurpRemoteText(
        text = stringResource(titleResource),
        style = tokens.typography.display,
        colour = tokens.colourScheme.contentPrimary,
        modifier = modifier,
    )
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun ScreenHeadingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        ScreenHeading(titleResource = R.string.navigation_section_dashboard)
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun ScreenHeadingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        ScreenHeading(titleResource = R.string.navigation_section_dashboard)
    }
}
