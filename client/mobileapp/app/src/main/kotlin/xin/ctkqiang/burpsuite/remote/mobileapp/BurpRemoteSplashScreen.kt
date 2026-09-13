package xin.ctkqiang.burpsuite.remote.mobileapp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteBrandMark
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 启动屏：标记落下 + 一圈转动，然后交给应用。
 *
 * 用品牌标记本身而不是一张位图：标记是矢量的，任何屏幕密度下都不会糊，
 * 而且它和应用图标、顶栏用的是同一套几何，三处看起来永远是同一个东西。
 *
 * 标记的入场用 spring 而不是定长缓动：落下时略微过冲再稳住，像被放到位置上，而不是被定时器推过去。
 * 背景填的是主题底色，因此从窗口底色切到启动屏、再从启动屏切到主界面，中间不会闪出第三种颜色。
 */
@Composable
fun BurpRemoteSplashScreen(modifier: Modifier = Modifier) {
    val tokens = LocalBurpRemoteDesignTokens.current

    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasEntered = true }

    // 单一来源：这个值只有一处写入，不存在互相取消的可能。
    val enterProgress by
        animateFloatAsState(
            targetValue = if (hasEntered) 1f else 0f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "splash-enter-progress",
        )
    // 环绕的那一圈只是「还在准备」，因此常驻匀速转，不参与入场动画。
    val ringAngle by
        rememberInfiniteTransition(label = "splash-ring-rotation").animateFloat(
            initialValue = 0f,
            targetValue = FULL_TURN_ANGLE,
            animationSpec = infiniteRepeatable(tween(RING_ROTATION_DURATION, easing = LinearEasing)),
            label = "splash-ring-angle",
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(tokens.colourScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(MARK_CONTAINER_SIZE),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePixels = RING_STROKE.toPx()
                val inset = strokePixels / 2f
                rotate(degrees = ringAngle) {
                    drawArc(
                        color = tokens.colourScheme.accent.copy(alpha = RING_ALPHA * enterProgress),
                        startAngle = RING_START_ANGLE,
                        sweepAngle = RING_SWEEP,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokePixels, size.height - strokePixels),
                        style = Stroke(width = strokePixels, cap = StrokeCap.Round),
                    )
                }
            }
            BurpRemoteBrandMark(
                modifier =
                    Modifier
                        .size(BurpRemoteSizing.StateIcon)
                        .graphicsLayer {
                            // 从稍小一档落到位；透明度跟着一起走，避免标记先出现再放大。
                            val scale = MARK_START_SCALE + (1f - MARK_START_SCALE) * enterProgress
                            scaleX = scale
                            scaleY = scale
                            alpha = enterProgress
                        },
                colour = tokens.colourScheme.accent,
            )
        }

        Spacer(modifier = Modifier.height(BurpRemoteSpacing.Medium))

        BurpRemoteText(
            text = stringResource(R.string.app_name),
            style = tokens.typography.title,
            colour = tokens.colourScheme.contentPrimary,
            modifier = Modifier.graphicsLayer { alpha = enterProgress },
        )
    }
}

private val MARK_CONTAINER_SIZE = 96.dp

private val RING_STROKE = 2.dp

// 只有一处写入的入场动画用 spring；这一圈常驻转动是匀速的，因此单独给时长。
private const val RING_ROTATION_DURATION = 1400
private const val RING_START_ANGLE = 0f
private const val RING_SWEEP = 90f
private const val RING_ALPHA = 0.28f
private const val FULL_TURN_ANGLE = 360f
private const val MARK_START_SCALE = 0.72f

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun BurpRemoteSplashScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) { BurpRemoteSplashScreen() }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun BurpRemoteSplashScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) { BurpRemoteSplashScreen() }
}
