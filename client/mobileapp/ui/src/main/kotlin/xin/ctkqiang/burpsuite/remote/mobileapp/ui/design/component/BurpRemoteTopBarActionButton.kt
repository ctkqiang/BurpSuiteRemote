package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 顶栏右侧的一个图标动作。
 *
 * 触感与配色在这里一次做对，调用方只给语义与行为；[contentDescription] 是必填的，因为顶栏图标
 * 没有文字，缺了它无障碍服务只能读出「按钮」。
 *
 * 图标 24dp 放在 48dp 的触控区里：图标能画小，手指不能；触控区比图标大出来的那圈不改变
 * 图标自身的对齐，因此相邻两个动作不会看起来比图标实际间距更远。
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

    Box(
        modifier =
            modifier
                .size(BurpRemoteSizing.MinimumTouchTarget)
                .clickable {
                    haptics.tap()
                    onClick()
                },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tokens.colourScheme.contentPrimary),
            modifier = Modifier.size(BurpRemoteSizing.Icon),
        )
    }
}
