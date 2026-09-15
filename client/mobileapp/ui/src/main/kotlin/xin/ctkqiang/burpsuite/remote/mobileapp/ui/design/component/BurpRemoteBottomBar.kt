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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 液态玻璃浮动底栏：圆角 30、左右边距 20、抬离手势条 12、各项等分、高 60dp。
 *
 * 分层靠五样东西（比原版多了一道内描边与投影）：
 * 1. Haze 实时模糊下方内容（底栏浮在内容之上，直接采样）
 * 2. 半透明表面色：模糊之外仍有自己的底色，不是"让底下内容透过来"
 * 3. 顶部高光渐变：玻璃反光——浅色下强、深色下弱
 * 4. 1dp 半透明外描边 + 内描边：在高对比内容之上仍能看出底栏边界，内描边制造"玻璃厚度"
 * 5. 底栏下方软投影：浮起感不靠阴影叠层，而靠这一道方向性投影
 *
 * 选中胶囊画在条目之下的独立一层：它只改自己的位置，不参与布局，因此切分区时没有任何一项会位移。
 * 选中项的图标同时放大一档并带强调色，未选中项退到次要字色。
 *
 * @param hazeState Scaffold 里创建的 HazeState；用来采集下方内容做液态玻璃。
 * @param items 分区条目；为空时整条底栏不画。
 * @param selectedRoute 当前选中的路由；它决定胶囊停在哪一项下面。
 * @param onSelect 选中回调，交回被点那一条的路由。
 */
@Composable
fun BurpRemoteBottomBar(
    hazeState: HazeState,
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

    val isDark = tokens.isDark

    // 液态玻璃底的半透明表面色：深色下偏亮（让 glass 有存在感），浅色下偏深（避免和背景融为一体）
    val surfaceTint =
        tokens.colourScheme.surfaceElevated.copy(
            alpha = if (isDark) GLASS_SURFACE_ALPHA_DARK else GLASS_SURFACE_ALPHA_LIGHT,
        )

    // 顶部高光：浅色下玻璃反光明显，深色下收弱——纯黑底上一道白高光会像一条裂缝。
    val highlightTopAlpha = if (isDark) GLASS_HIGHLIGHT_TOP_ALPHA_DARK else GLASS_HIGHLIGHT_TOP_ALPHA_LIGHT
    // 底部内阴影：模拟玻璃下沿的厚度与环境光遮蔽。
    val shadowBottomAlpha = if (isDark) GLASS_SHADOW_BOTTOM_ALPHA_DARK else GLASS_SHADOW_BOTTOM_ALPHA_LIGHT
    // 外描边：深色下亮一点（让边在深底上可见），浅色下暗一点。
    val outlineAlpha = if (isDark) GLASS_OUTLINE_ALPHA_DARK else GLASS_OUTLINE_ALPHA_LIGHT

    Box(
        modifier =
            Modifier
                .padding(
                    start = BurpRemoteSizing.BottomBarHorizontalMargin,
                    end = BurpRemoteSizing.BottomBarHorizontalMargin,
                    bottom = BurpRemoteSizing.BottomBarGestureGap,
                )
                .fillMaxWidth()
                // 浮起投影：方向向下、扩散大、透明度低，模拟玻璃悬浮在内容之上的环境光遮挡。
                .shadow(
                    elevation = GLASS_FLOATING_ELEVATION,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = GLASS_SHADOW_AMBIENT_ALPHA),
                    spotColor = Color.Black.copy(alpha = GLASS_SHADOW_SPOT_ALPHA),
                )
                .height(BurpRemoteSizing.BottomBarHeight)
                .clip(shape)
                .hazeChild(
                    state = hazeState,
                    style =
                        HazeStyle(
                            backgroundColor = surfaceTint,
                            blurRadius = GLASS_BLUR_RADIUS,
                            noiseFactor = GLASS_NOISE,
                            tints = emptyList(),
                        ),
                )
                .border(
                    width = BurpRemoteSizing.Divider,
                    color = tokens.colourScheme.outline.copy(alpha = outlineAlpha),
                    shape = shape,
                ),
    ) {
        // 顶部高光渐变：从上往下由亮到透明，模拟玻璃对光线的反射
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = highlightTopAlpha),
                                    Color.White.copy(alpha = 0f),
                                ),
                        ),
                    ),
        )

        // 底部内阴影渐变：从下往上由暗到透明，制造玻璃厚度与环境光遮蔽
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color.Black.copy(alpha = 0f),
                                    Color.Black.copy(alpha = shadowBottomAlpha),
                                ),
                        ),
                    ),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // 选中胶囊：强调色半透明填充 + 内描边，浮在条目之下。
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = indicatorOffset)
                        .width(entryWidth)
                        .fillMaxHeight()
                        .padding(BurpRemoteSizing.BottomBarIndicatorInset)
                        .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        tokens.colourScheme.accent.copy(alpha = INDICATOR_FILL_ALPHA_TOP),
                                        tokens.colourScheme.accent.copy(alpha = INDICATOR_FILL_ALPHA_BOTTOM),
                                    ),
                            ),
                        )
                        .border(
                            width = BurpRemoteSizing.Divider,
                            color = tokens.colourScheme.accent.copy(alpha = INDICATOR_BORDER_ALPHA),
                            shape = RoundedCornerShape(BurpRemoteRadius.Capsule),
                        ),
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
    val labelAlpha by
        animateFloatAsState(
            targetValue = if (isSelected) SELECTED_LABEL_ALPHA else UNSELECTED_LABEL_ALPHA,
            animationSpec = tween(BurpRemoteMotion.DURATION_FAST, easing = BurpRemoteMotion.EasingStandard),
            label = "bottom-bar-label-alpha",
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
            modifier = Modifier.graphicsLayer { alpha = labelAlpha },
        )
    }
}

// ── 液态玻璃参数 ───────────────────────────────────────────────────────────
// 模糊半径 28dp 足够看出底下内容又不会糊成一团；噪点 0.18 让玻璃有真实的颗粒感但不抢眼。
private val GLASS_BLUR_RADIUS = 28.dp
private const val GLASS_NOISE = 0.18f

// 表面色透明度：浅色下 0.60（底色保留存在感但不糊住内容），深色下 0.72（深底上要更亮才看得出玻璃）。
private const val GLASS_SURFACE_ALPHA_LIGHT = 0.60f
private const val GLASS_SURFACE_ALPHA_DARK = 0.72f

// 顶部高光：浅色下 0.22（明显反光），深色下 0.08（深底上强光会像裂缝）。
private const val GLASS_HIGHLIGHT_TOP_ALPHA_LIGHT = 0.22f
private const val GLASS_HIGHLIGHT_TOP_ALPHA_DARK = 0.08f

// 底部内阴影：浅色下 0.06，深色下 0.18（深底需要更强的厚度暗示）。
private const val GLASS_SHADOW_BOTTOM_ALPHA_LIGHT = 0.06f
private const val GLASS_SHADOW_BOTTOM_ALPHA_DARK = 0.18f

// 外描边透明度：浅色下 0.30，深色下 0.45（深底上需要更亮的边才可见）。
private const val GLASS_OUTLINE_ALPHA_LIGHT = 0.30f
private const val GLASS_OUTLINE_ALPHA_DARK = 0.45f

// 浮起投影：环境光与聚光分开控制，整体偏软。
private val GLASS_FLOATING_ELEVATION = 16.dp
private const val GLASS_SHADOW_AMBIENT_ALPHA = 0.12f
private const val GLASS_SHADOW_SPOT_ALPHA = 0.10f

// 选中胶囊：上浅下深的渐变填充 + 一圈半透明强调色描边。
private const val INDICATOR_FILL_ALPHA_TOP = 0.18f
private const val INDICATOR_FILL_ALPHA_BOTTOM = 0.10f
private const val INDICATOR_BORDER_ALPHA = 0.30f

private const val SELECTED_ICON_SCALE = 1.12f
private const val UNSELECTED_ICON_SCALE = 1f
private const val SELECTED_LABEL_ALPHA = 1f
private const val UNSELECTED_LABEL_ALPHA = 0.75f
