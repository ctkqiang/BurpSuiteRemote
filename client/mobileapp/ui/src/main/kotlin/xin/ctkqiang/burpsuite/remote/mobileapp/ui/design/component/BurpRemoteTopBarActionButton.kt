package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 顶栏右侧的一个图标动作。
 *
 * 触感与配色在这里一次做对，调用方只给语义与行为；[contentDescription] 是必填的，因为顶栏图标
 * 没有文字，缺了它无障碍服务只能读出「按钮」。
 */
@Composable
fun BurpRemoteTopBarActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()

    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tokens.colourScheme.contentPrimary),
        modifier =
            modifier
                .size(ACTION_ICON_SIZE)
                .clickable {
                    haptics.tap()
                    onClick()
                },
    )
}

private val ACTION_ICON_SIZE = 24.dp
