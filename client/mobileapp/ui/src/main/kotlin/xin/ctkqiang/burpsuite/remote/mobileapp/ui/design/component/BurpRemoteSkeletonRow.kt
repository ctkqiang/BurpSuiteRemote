package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 骨架行：列表加载中的占位。
 *
 * 用灰块而不是转圈：转圈只说「在忙」，骨架行已经把「马上要出现的是两行、长什么样」先摆好了，
 * 内容落进来时布局不会整屏跳一次。调用方按需要的行数重复它即可。
 *
 * 微光是按帧画出来的，进度通过 lambda 传进绘制阶段，动画因此只触发重绘、不触发重组——
 * 一屏十几行一起重组会把首帧拖出来。
 */
@Composable
fun BurpRemoteSkeletonRow(modifier: Modifier = Modifier) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val shimmerProgress =
        rememberInfiniteTransition(label = "skeleton-shimmer").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(SHIMMER_DURATION, easing = LinearEasing)),
            label = "skeleton-shimmer-progress",
        )
    val shimmer = { shimmerProgress.value }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = BurpRemoteSpacing.ListRowVertical),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
    ) {
        SkeletonBar(
            widthFraction = TITLE_WIDTH_FRACTION,
            shimmerProgress = shimmer,
            baseColour = tokens.colourScheme.surface,
            highlightColour = tokens.colourScheme.outline,
        )
        SkeletonBar(
            widthFraction = DETAIL_WIDTH_FRACTION,
            shimmerProgress = shimmer,
            baseColour = tokens.colourScheme.surface,
            highlightColour = tokens.colourScheme.outline,
        )
    }
}

// 高光带扫过整块：两端都收成透明，块边缘才不会出现一道硬边。
@Composable
private fun SkeletonBar(
    widthFraction: Float,
    shimmerProgress: () -> Float,
    baseColour: Color,
    highlightColour: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth(widthFraction)
                .height(SKELETON_BAR_HEIGHT)
                .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                .background(baseColour)
                .drawBehind {
                    val sweepWidth = size.width * SWEEP_WIDTH_FRACTION
                    val centre = -sweepWidth + (size.width + 2 * sweepWidth) * shimmerProgress()
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, highlightColour, Color.Transparent),
                                startX = centre - sweepWidth / 2f,
                                endX = centre + sweepWidth / 2f,
                            ),
                    )
                },
    )
}

private val SKELETON_BAR_HEIGHT = 14.dp
private const val TITLE_WIDTH_FRACTION = 0.45f
private const val DETAIL_WIDTH_FRACTION = 0.75f
private const val SWEEP_WIDTH_FRACTION = 0.6f
private const val SHIMMER_DURATION = 1200
