package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 存储设置（plan §47、plan §58）。
 *
 * 这一屏说明本地数据放在哪、什么时候会被清掉。清库与迁移都还没有端口，因此这里先讲清楚现状，
 * 不放一个点了不生效的「清空」按钮。
 */
@Composable
fun StorageScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        run {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = BurpRemoteSpacing.Large,
                            vertical = BurpRemoteSpacing.Large,
                        ),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
            ) {
                Section(
                    headingResource = R.string.settings_storage_database_heading,
                    content = {
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.settings_storage_database_headline),
                            detail = stringResource(R.string.settings_storage_database_body),
                        )
                    },
                )
                Section(
                    headingResource = R.string.settings_storage_export_heading,
                    content = {
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.settings_storage_export_headline),
                            detail = stringResource(R.string.settings_storage_export_body),
                        )
                    },
                )
                Section(
                    headingResource = R.string.settings_storage_maintenance_heading,
                    content = {
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.settings_storage_reason_heading),
                            detail = stringResource(R.string.settings_storage_reason),
                        )
                    },
                )
            }
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
