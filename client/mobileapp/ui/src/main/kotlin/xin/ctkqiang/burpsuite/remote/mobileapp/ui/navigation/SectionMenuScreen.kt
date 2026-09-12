package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/** 分区索引屏：把 plan §43 里某个分区的子屏列出来。无状态，点哪一行由装配层决定。 */
@Composable
fun SectionMenuScreen(
    @StringRes titleResource: Int,
    entries: List<SectionMenuEntry>,
    onOpenRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        ) {
            Text(text = stringResource(titleResource), style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.padding(top = SECTION_SPACING)) {
                entries.forEach { entry -> SectionMenuEntryRow(entry = entry, onOpenRoute = onOpenRoute) }
            }
        }
    }
}

@Composable
private fun SectionMenuEntryRow(
    entry: SectionMenuEntry,
    onOpenRoute: (String) -> Unit,
) {
    Text(
        text = stringResource(entry.labelResource),
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onOpenRoute(entry.route) }
                .padding(vertical = ROW_PADDING),
    )
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun SectionMenuScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        SectionMenuScreen(
            titleResource = TopLevelSection.Live.labelResource,
            entries = TopLevelSection.Live.indexEntries,
            onOpenRoute = {},
        )
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun SectionMenuScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        SectionMenuScreen(
            titleResource = TopLevelSection.Live.labelResource,
            entries = TopLevelSection.Live.indexEntries,
            onOpenRoute = {},
        )
    }
}

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val SECTION_SPACING = 16.dp
private val ROW_PADDING = 12.dp
