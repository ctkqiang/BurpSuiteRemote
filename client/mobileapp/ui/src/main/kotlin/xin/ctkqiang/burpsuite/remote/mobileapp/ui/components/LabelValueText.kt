package xin.ctkqiang.burpsuite.remote.mobileapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 「标签 + 取值」两行。
 *
 * 取值走等宽字：这一栏放的是主机、标识符、序号这类要逐字符比对的东西（rules.md §10 之外见 plan §45）。
 */
@Composable
fun LabelValueText(
    @StringRes labelResource: Int,
    value: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    Column(modifier = modifier.fillMaxWidth()) {
        BurpRemoteText(
            text = stringResource(labelResource),
            style = tokens.typography.label,
            colour = tokens.colourScheme.contentSecondary,
            maxLines = 1,
        )
        BurpRemoteText(
            text = value,
            style = tokens.typography.technical,
            colour = tokens.colourScheme.contentPrimary,
            modifier = Modifier.padding(top = BurpRemoteSpacing.ExtraSmall),
        )
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun LabelValueTextLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        LabelValueText(labelResource = R.string.navigation_entry_live_history, value = "GET /api/user")
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun LabelValueTextDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        LabelValueText(labelResource = R.string.navigation_entry_live_history, value = "GET /api/user")
    }
}
