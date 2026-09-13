package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 存储设置（plan §47、plan §58）。
 *
 * 这一屏讲清本地数据放在哪、导出写到哪、以及哪些维护动作还没有端口。清库与迁移都还没有端口，
 * 因此不放一个点了不生效的「清空」按钮（rules.md §5.1）。
 *
 * 排版与安全屏共用同一个 [Section]：两屏都是「一段标题 + 一条陈述」，各自画一套只会让它们慢慢长歪。
 */
@Composable
fun StorageScreen(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = BurpRemoteSpacing.ScreenEdge,
                    vertical = BurpRemoteSpacing.ExtraLarge,
                ),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraLarge),
    ) {
        Section(
            headingResource = R.string.settings_storage_database_heading,
            titleResource = R.string.settings_storage_database_headline,
            detailResource = R.string.settings_storage_database_body,
        )
        Section(
            headingResource = R.string.settings_storage_export_heading,
            titleResource = R.string.settings_storage_export_headline,
            detailResource = R.string.settings_storage_export_body,
        )
        Section(
            headingResource = R.string.settings_storage_maintenance_heading,
            titleResource = R.string.settings_storage_reason_heading,
            detailResource = R.string.settings_storage_reason,
        )
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
