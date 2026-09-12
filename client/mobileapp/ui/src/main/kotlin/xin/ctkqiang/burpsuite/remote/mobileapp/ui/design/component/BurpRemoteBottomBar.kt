package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteDesignTokens
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 浮动底栏：圆角 20、玻璃质感、左右留边距，浮在内容之上。
 *
 * 真模糊只在 API 31 起可用（RenderEffect）。低版本退化为半透明加渐变加描边——照抄高版本的透明度
 * 会让底栏在低版本上透成一团糊，而直接画成实心块又丢掉了「浮在内容之上」这件事。
 *
 * 选中项背后有一块滑动的胶囊指示器：只靠颜色变化，用户在余光里看不出「刚才从哪一栏切到了哪一栏」。
 * 条目为空时整条底栏不画——二级页面本来就不该有它，画一条空壳等于告诉用户这里少了点什么。
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
    val isBlurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var trackWidthPixels by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val entryWidth: Dp = with(density) { (trackWidthPixels / items.size).toDp() }
    val selectedIndex = items.indexOfFirst { item -> item.route == selectedRoute }.coerceAtLeast(0)
    val indicatorOffset by
        animateDpAsState(
            targetValue = entryWidth * selectedIndex,
            animationSpec = tween(BurpRemoteMotion.DURATION_REGULAR, easing = BurpRemoteMotion.EasingStandard),
            label = "bottom-bar-indicator-offset",
        )

    Box(
        modifier =
            Modifier
                .padding(horizontal = BurpRemoteSpacing.Large, vertical = BurpRemoteSpacing.Small)
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .shadow(elevation = BAR_SHADOW_ELEVATION, shape = shape, clip = false)
                .clip(shape)
                .burpRemoteGlassSurface(tokens = tokens, shape = shape, isBlurAvailable = isBlurAvailable),
    ) {
        if (isBlurAvailable) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .blur(GLASS_BLUR_RADIUS)
                        .background(tokens.colourScheme.surfaceElevated.copy(alpha = GLASS_SHEEN_ALPHA)),
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorOffset)
                        .width(entryWidth)
                        .fillMaxHeight()
                        .padding(horizontal = ENTRY_GAP / 2, vertical = INDICATOR_INSET)
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

    Column(
        modifier =
            Modifier
                .weight(weight = 1f, fill = true)
                .fillMaxHeight()
                .padding(horizontal = ENTRY_GAP / 2)
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
            modifier = Modifier.size(ENTRY_ICON_SIZE),
        )
        Spacer(modifier = Modifier.height(BurpRemoteSpacing.ExtraSmall))
        BurpRemoteText(
            text = stringResource(item.labelResource),
            style = tokens.typography.label,
            colour = entryColour,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// 玻璃层只做两件事：给底色，给描边；高版本的柔化由上面的模糊层负责。
@Composable
private fun Modifier.burpRemoteGlassSurface(
    tokens: BurpRemoteDesignTokens,
    shape: Shape,
    isBlurAvailable: Boolean,
): Modifier {
    val glassAlpha = if (isBlurAvailable) GLASS_ALPHA_BLURRED else GLASS_ALPHA_PLAIN
    return this
        .background(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            tokens.colourScheme.surfaceElevated.copy(alpha = glassAlpha),
                            tokens.colourScheme.surface.copy(alpha = glassAlpha),
                        ),
                ),
            shape = shape,
        )
        .border(
            width = GLASS_BORDER_WIDTH,
            color = tokens.colourScheme.outline.copy(alpha = GLASS_BORDER_ALPHA),
            shape = shape,
        )
}

private val BAR_HEIGHT = 64.dp
private val ENTRY_ICON_SIZE = 22.dp
private val BAR_SHADOW_ELEVATION = 12.dp
private val ENTRY_GAP = 4.dp
private val INDICATOR_INSET = 6.dp
private val GLASS_BORDER_WIDTH = 1.dp
private val GLASS_BLUR_RADIUS = 24.dp
private const val GLASS_ALPHA_BLURRED = 0.82f
private const val GLASS_ALPHA_PLAIN = 0.96f
private const val GLASS_SHEEN_ALPHA = 0.04f
private const val GLASS_BORDER_ALPHA = 0.6f
private const val INDICATOR_FILL_ALPHA = 0.14f
