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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItem
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItemGroup
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

/**
 * 分区索引屏：把 plan §43 里某个分区的子屏列出来。无状态，点哪一行由装配层决定。
 *
 * 行按 [SectionMenuEntry.groupResource] 分组，每组一个标题、一块描边容器，组内几行共用一条分隔线。
 * 分组的理由是数量：五个平铺的入口读起来是一堆选项，分成「连接 / 界面 / 数据与安全」三档之后，
 * 用户找的是「哪一类」而不是「第几个」。分组顺序取首次出现的顺序，与 [entries] 的排列一致。
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
                    horizontal = BurpRemoteSpacing.ScreenEdge,
                    vertical = BurpRemoteSpacing.ExtraLarge,
                ),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
    ) {
        entries.groupBy { entry -> entry.groupResource }.forEach { (groupResource, groupEntries) ->
            Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
                if (groupResource != null) {
                    BurpRemoteSectionHeading(text = stringResource(groupResource))
                }
                BurpRemoteListItemGroup {
                    groupEntries.forEachIndexed { index, entry ->
                        BurpRemoteListItem(
                            title = stringResource(entry.labelResource),
                            subtitle = stringResource(entry.descriptionResource),
                            showsChevron = true,
                            showsDivider = index != groupEntries.lastIndex,
                            onClick = { onOpenRoute(entry.route) },
                        )
                    }
                }
            }
        }
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun SectionMenuScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        SectionMenuScreen(
            entries = TopLevelSection.Settings.indexEntries,
            onOpenRoute = {},
        )
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun SectionMenuScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        SectionMenuScreen(
            entries = TopLevelSection.Settings.indexEntries,
            onOpenRoute = {},
        )
    }
}
