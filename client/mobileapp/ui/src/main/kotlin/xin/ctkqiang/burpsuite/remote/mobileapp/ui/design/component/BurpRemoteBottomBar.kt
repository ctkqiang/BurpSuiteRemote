package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 浮动底栏：圆角 30、左右边距 20、抬离手势条 12、各项等分、高 60dp。
 *
 * 圆角与高度相等的一半（30dp = 60dp / 2），两端因此是两个完整的半圆；这是半径的上限，
 * 再大也只是被裁掉，所以它同时是「最圆」的那一档。
 *
 * 分层靠三样真实存在的东西：不透明的抬升表面色、1dp 描边、一道浅投影。这里刻意不做「玻璃模糊」——
 * Compose 的 `Modifier.blur` 模糊的是自己画出来的内容，没接系统背景模糊 API 的情况下，对一块底色做模糊
 * 得到的仍是那块底色，只会让代码看起来比它实际做的多。
 *
 * 选中胶囊画在条目之下的独立一层：它只改自己的位置，不参与布局，因此切分区时没有任何一项会位移。
 * 选中项的图标同时放大一档，这样在只有两三个分区的底栏上也能一眼看出当前在哪。
 *
 * @param items 分区条目；为空时整条底栏不画。
 * @param selectedRoute 当前选中的路由；它决定胶囊停在哪一项下面。
 * @param onSelect 选中回调，交回被点那一条的路由。
 */
@Composable
fun BurpRemoteBottomBar(
    items: List<BurpRemoteBottomBarItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
) {
    if (items.isEmpty()) return

    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()
    val shape = RoundedCornerShape(BurpRemoteRadius.FloatingNavigation)
    var trackWidthPixels by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val entryWidth: Dp = with(density) { (trackWidthPixels / items.size).toDp() }
    val selectedIndex = items.indexOfFirst { item -> item.route == selectedRoute }.coerceAtLeast(0)
    val indicatorOffset by
        animateDpAsState(
            targetValue = entryWidth * selectedIndex,
            animationSpec = BurpRemoteMotion.SpringIndicator,
            label = "bottom-bar-indicator-offset",
        )

    Box(
        modifier =
            Modifier
                .padding(
                    start = BurpRemoteSizing.BottomBarHorizontalMargin,
                    end = BurpRemoteSizing.BottomBarHorizontalMargin,
                    bottom = BurpRemoteSizing.BottomBarGestureGap,
                )
                .fillMaxWidth()
                .height(BurpRemoteSizing.BottomBarHeight)
                .shadow(elevation = BAR_SHADOW_ELEVATION, shape = shape, clip = false)
                .clip(shape)
                .background(color = tokens.colourScheme.surfaceElevated, shape = shape)
                .border(width = BurpRemoteSizing.Divider, color = tokens.colourScheme.outline, shape = shape),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorOffset)
                        .width(entryWidth)
                        .fillMaxHeight()
                        .padding(BurpRemoteSizing.BottomBarIndicatorInset)
                        .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                        .background(tokens.colourScheme.accent.copy(alpha = INDICATOR_FILL_ALPHA)),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onSizeChanged { measuredSize -> trackWidthPixels = measuredSize.width },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    BottomBarEntry(
                        item = item,
                        isSelected = item.route == selectedRoute,
                        haptics = haptics,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarEntry(
    item: BurpRemoteBottomBarItem,
    isSelected: Boolean,
    haptics: BurpRemoteHaptics,
    onSelect: (String) -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val entryColour by
        animateColorAsState(
            targetValue = if (isSelected) tokens.colourScheme.accent else tokens.colourScheme.contentSecondary,
            animationSpec = tween(BurpRemoteMotion.DURATION_FAST, easing = BurpRemoteMotion.EasingStandard),
            label = "bottom-bar-entry-colour",
        )
    val iconScale by
        animateFloatAsState(
            targetValue = if (isSelected) SELECTED_ICON_SCALE else UNSELECTED_ICON_SCALE,
            animationSpec = tween(BurpRemoteMotion.DURATION_FAST, easing = BurpRemoteMotion.EasingStandard),
            label = "bottom-bar-icon-scale",
        )

    Column(
        modifier =
            Modifier
                .weight(weight = 1f, fill = true)
                .fillMaxHeight()
                .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                .clickable {
                    haptics.select()
                    onSelect(item.route)
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = rememberVectorPainter(item.icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(entryColour),
            modifier =
                Modifier
                    .size(BurpRemoteSizing.Icon)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
        )
        Spacer(modifier = Modifier.height(BurpRemoteSpacing.ExtraSmall))
        BurpRemoteText(
            text = stringResource(item.labelResource),
            style = tokens.typography.caption,
            colour = entryColour,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 投影只用来把底栏从内容里抬起来；纯黑底上看不见它，也没有害处。
private val BAR_SHADOW_ELEVATION = 10.dp

private const val INDICATOR_FILL_ALPHA = 0.12f
private const val SELECTED_ICON_SCALE = 1.1f
private const val UNSELECTED_ICON_SCALE = 1f
