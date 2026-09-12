package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.NotImplementedReasonText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.SectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 存储设置（plan §47 的一项）。
 *
 * 这一屏说明本地数据放在哪、什么时候会被清掉。清库与迁移都还没有端口，因此这里先讲清楚现状，
 * 不放一个点了不生效的「清空」按钮（plan §58）。
 */
@Composable
fun StorageScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            ScreenHeading(titleResource = R.string.settings_storage_title)
            Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
                SectionHeading(titleResource = R.string.settings_storage_database_heading)
                Text(
                    text = stringResource(R.string.settings_storage_database_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
                SectionHeading(titleResource = R.string.settings_storage_export_heading)
                Text(
                    text = stringResource(R.string.settings_storage_export_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            NotImplementedReasonText(reasonResource = R.string.settings_storage_reason)
        }
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun StorageScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) { StorageScreen() }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun StorageScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) { StorageScreen() }
}

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
