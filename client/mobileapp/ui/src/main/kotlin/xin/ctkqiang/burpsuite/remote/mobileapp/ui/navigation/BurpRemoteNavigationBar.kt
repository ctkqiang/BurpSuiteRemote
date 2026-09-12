package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** 一级分区的底栏。无状态：当前分区与点击行为都由装配层给。 */
@Composable
fun BurpRemoteNavigationBar(
    selectedSection: TopLevelSection?,
    onSectionSelected: (TopLevelSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        TopLevelSection.entries.forEach { section ->
            NavigationBarItem(
                selected = section == selectedSection,
                onClick = { onSectionSelected(section) },
                // 标签已经写出分区名，读屏再念一次图标名只是重复。
                icon = { Icon(imageVector = section.icon, contentDescription = null) },
                label = { Text(text = stringResource(section.labelResource)) },
            )
        }
    }
}
