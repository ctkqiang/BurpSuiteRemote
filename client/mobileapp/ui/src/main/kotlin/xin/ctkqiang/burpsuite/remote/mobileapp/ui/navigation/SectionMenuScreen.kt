package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.compose.foundation.layout.Arrangement
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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 分区索引屏：把 plan §43 里某个分区的子屏列出来。无状态，点哪一行由装配层决定。
 *
 * 这一屏不画标题：标题归外壳那一块唯一的顶栏，同层级再画一次等于一屏两个标题。
 */
@Composable
fun SectionMenuScreen(
    entries: List<SectionMenuEntry>,
    onOpenRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = BurpRemoteSpacing.Large,
                    vertical = BurpRemoteSpacing.ExtraLarge,
                ),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
    ) {
        entries.forEach { entry ->
            SectionMenuEntryRow(entry = entry, onOpenRoute = onOpenRoute)
        }
    }
}

@Composable
private fun SectionMenuEntryRow(
    entry: SectionMenuEntry,
    onOpenRoute: (String) -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    BurpRemoteCard(
        isInteractive = true,
        onClick = { onOpenRoute(entry.route) },
        content = {
            BurpRemoteText(
                text = stringResource(entry.labelResource),
                style = tokens.typography.body,
                colour = tokens.colourScheme.contentPrimary,
            )
        },
    )
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun SectionMenuScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        SectionMenuScreen(
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
            entries = TopLevelSection.Live.indexEntries,
            onOpenRoute = {},
        )
    }
}
