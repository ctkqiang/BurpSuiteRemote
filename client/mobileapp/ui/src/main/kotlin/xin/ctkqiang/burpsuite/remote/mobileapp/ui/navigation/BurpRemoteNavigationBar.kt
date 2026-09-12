package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteBottomBar
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteBottomBarItem
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics

/**
 * 一级分区的浮动底栏。无状态：当前分区与点击行为都由装配层给。
 *
 * 条目直接从 [TopLevelSection] 投影出来，因此底栏永远与导航模型一致，不额外维护一张条目表。
 */
@Composable
fun BurpRemoteNavigationBar(
    selectedSection: TopLevelSection?,
    onSectionSelected: (TopLevelSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()
    val items =
        TopLevelSection.entries.map { section ->
            BurpRemoteBottomBarItem(
                route = section.landingRoute,
                labelResource = section.labelResource,
                icon = section.icon,
            )
        }

    Box(modifier = modifier.fillMaxWidth()) {
        BurpRemoteBottomBar(
            items = items,
            selectedRoute = selectedSection?.landingRoute.orEmpty(),
            onSelect = { route ->
                val section = TopLevelSection.entries.firstOrNull { entry -> entry.landingRoute == route }
                if (section != null) {
                    haptics.select()
                    onSectionSelected(section)
                }
            },
        )
    }
}
