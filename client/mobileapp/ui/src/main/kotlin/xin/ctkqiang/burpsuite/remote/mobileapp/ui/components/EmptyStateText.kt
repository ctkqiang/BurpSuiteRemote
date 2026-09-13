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

/**
 * 空状态说明。
 *
 * 空列表显示的必须是「为什么空」，不是一句无声的「没有数据」——用户据此才知道下一步该做什么。
 */
@Composable
fun EmptyStateText(
    @StringRes messageResource: Int,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    BurpRemoteText(
        text = stringResource(messageResource),
        style = tokens.typography.label,
        colour = tokens.colourScheme.contentSecondary,
        modifier = modifier,
    )
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun EmptyStateTextLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        EmptyStateText(messageResource = R.string.components_absent_value)
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun EmptyStateTextDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        EmptyStateText(messageResource = R.string.components_absent_value)
    }
}
