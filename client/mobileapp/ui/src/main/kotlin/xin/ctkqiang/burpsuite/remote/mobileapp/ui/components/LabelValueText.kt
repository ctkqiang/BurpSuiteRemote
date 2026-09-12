package xin.ctkqiang.burpsuite.remote.mobileapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 「标签 + 取值」两行。
 *
 * 各屏的元数据字段都用它，免得每屏各写一套排版；标签走资源，取值由调用方给（rules.md §9）。
 */
@Composable
fun LabelValueText(
    @StringRes labelResource: Int,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(labelResource),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
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
