package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 按钮。
 *
 * 不可用时保留在原来的位置上并压低透明度，而不是从布局里抽掉：按钮一旦消失，用户会以为功能也没了。
 */
@Composable
fun BurpRemoteButton(
    text: String,
    onClick: () -> Unit,
    style: BurpRemoteButtonStyle,
    isEnabled: Boolean = true,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()
    val shape = RoundedCornerShape(BurpRemoteRadius.Control)
    val containerColour = containerColourFor(style, tokens.colourScheme)
    val contentColour = contentColourFor(style, tokens.colourScheme)

    Box(
        modifier =
            Modifier
                .clip(shape)
                .background(color = containerColour, shape = shape)
                .then(
                    if (style == BurpRemoteButtonStyle.Secondary) {
                        Modifier.border(width = 1.dp, color = tokens.colourScheme.outline, shape = shape)
                    } else {
                        Modifier
                    },
                )
                .clickable(enabled = isEnabled) {
                    haptics.tap()
                    onClick()
                }
                .alpha(if (isEnabled) ENABLED_ALPHA else DISABLED_ALPHA)
                .defaultMinSize(minHeight = BurpRemoteSizing.MinimumTouchTarget)
                .padding(horizontal = BurpRemoteSpacing.ScreenEdge, vertical = BurpRemoteSpacing.Medium),
        contentAlignment = Alignment.Center,
    ) {
        BurpRemoteText(
            text = text,
            style = tokens.typography.label,
            colour = contentColour,
            maxLines = 1,
        )
    }
}

private fun containerColourFor(
    style: BurpRemoteButtonStyle,
    colourScheme: BurpRemoteColourScheme,
): Color =
    when (style) {
        BurpRemoteButtonStyle.Primary -> colourScheme.accent
        BurpRemoteButtonStyle.Secondary -> colourScheme.surfaceElevated
        BurpRemoteButtonStyle.Ghost -> Color.Transparent
        BurpRemoteButtonStyle.Danger -> colourScheme.danger
    }

// 高饱和底上的字色都取 onAccent：它本来就是「压在强调色容器上的字」这一档，危险色同属这一类。
private fun contentColourFor(
    style: BurpRemoteButtonStyle,
    colourScheme: BurpRemoteColourScheme,
): Color =
    when (style) {
        BurpRemoteButtonStyle.Primary -> colourScheme.onAccent
        BurpRemoteButtonStyle.Secondary -> colourScheme.contentPrimary
        BurpRemoteButtonStyle.Ghost -> colourScheme.accent
        BurpRemoteButtonStyle.Danger -> colourScheme.onAccent
    }

private const val ENABLED_ALPHA = 1f
private const val DISABLED_ALPHA = 0.38f
