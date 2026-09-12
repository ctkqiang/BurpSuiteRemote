package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 下拉刷新。
 *
 * 自己实现而不是取 Material 3 的那一个：设计系统不引 Material 3，而刷新指示器是第一个必须换掉的东西。
 * 指示器画在内容之上、内容整体下移，因此不存在「把列表项压扁」的中间态。
 */
@Composable
fun BurpRemotePullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(0f) }
    val onRefreshState = rememberUpdatedState(onRefresh)
    val triggerOffsetPixels = with(density) { TRIGGER_OFFSET.toPx() }
    val restingOffsetPixels = with(density) { RESTING_OFFSET.toPx() }
    val maximumOffsetPixels = with(density) { MAXIMUM_OFFSET.toPx() }

    // 刷新结束就把指示器收回去；不回头收的话它会在下一次下拉之前一直占着位置。
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullOffset.value > 0f) {
            pullOffset.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(BurpRemoteMotion.DURATION_REGULAR, easing = BurpRemoteMotion.EasingStandard),
            )
        }
    }

    val nestedScrollConnection =
        remember(isRefreshing, coroutineScope) {
            object : NestedScrollConnection {
                // 只有内容已经到顶、还在继续往下拉时才算下拉；否则会跟列表自身的滚动打架。
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (isRefreshing || available.y <= 0f) return Offset.Zero
                    coroutineScope.launch {
                        val draggedOffset = pullOffset.value + available.y * DRAG_RESISTANCE
                        pullOffset.snapTo(draggedOffset.coerceIn(0f, maximumOffsetPixels))
                    }
                    return Offset(x = 0f, y = available.y)
                }

                // 反向滑动先把下拉位移收回去，别让用户以为列表卡住了。
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (available.y >= 0f || pullOffset.value <= 0f) return Offset.Zero
                    val consumedY = available.y.coerceAtLeast(-pullOffset.value)
                    coroutineScope.launch { pullOffset.snapTo((pullOffset.value + consumedY).coerceAtLeast(0f)) }
                    return Offset(x = 0f, y = consumedY)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    when {
                        pullOffset.value >= triggerOffsetPixels -> {
                            pullOffset.animateTo(
                                targetValue = restingOffsetPixels,
                                animationSpec =
                                    tween(
                                        BurpRemoteMotion.DURATION_FAST,
                                        easing = BurpRemoteMotion.EasingStandard,
                                    ),
                            )
                            onRefreshState.value()
                        }

                        pullOffset.value > 0f ->
                            pullOffset.animateTo(
                                targetValue = 0f,
                                animationSpec =
                                    tween(
                                        BurpRemoteMotion.DURATION_REGULAR,
                                        easing = BurpRemoteMotion.EasingStandard,
                                    ),
                            )
                    }
                    return Velocity.Zero
                }
            }
        }

    // 下拉进度只驱动指示器；内容整体下移走的 graphicsLayer，因此拖动期间不会重建列表布局。
    val pullProgress by
        produceState(initialValue = 0f, pullOffset, triggerOffsetPixels) {
            snapshotFlow { (pullOffset.value / triggerOffsetPixels).coerceIn(0f, 1f) }
                .collect { progress -> value = progress }
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
    ) {
        PullToRefreshIndicator(
            progress = pullProgress,
            isRefreshing = isRefreshing,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = BurpRemoteSpacing.Small),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = pullOffset.value },
        ) {
            content()
        }
    }
}

@Composable
private fun PullToRefreshIndicator(
    progress: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val indicatorColour = tokens.colourScheme.accent
    val rotation by
        rememberInfiniteTransition(label = "pull-to-refresh-rotation").animateFloat(
            initialValue = START_ANGLE,
            targetValue = FULL_TURN_ANGLE,
            animationSpec = infiniteRepeatable(tween(ROTATION_DURATION, easing = LinearEasing)),
            label = "pull-to-refresh-rotation-angle",
        )

    Canvas(modifier = modifier.size(INDICATOR_SIZE)) {
        val strokePixels = INDICATOR_STROKE.toPx()
        val arcDiameter = size.minDimension - strokePixels
        val arcTopLeft = Offset(strokePixels / 2f, strokePixels / 2f)
        val arcSize = Size(arcDiameter, arcDiameter)

        if (isRefreshing) {
            rotate(degrees = rotation) {
                drawArc(
                    color = indicatorColour,
                    startAngle = 0f,
                    sweepAngle = PARTIAL_ARC_SWEEP,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokePixels, cap = StrokeCap.Round),
                )
            }
        } else {
            drawArc(
                color = indicatorColour.copy(alpha = progress),
                startAngle = START_ANGLE,
                sweepAngle = PARTIAL_ARC_SWEEP * progress,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokePixels, cap = StrokeCap.Round),
            )
        }
    }
}

private val INDICATOR_SIZE = 28.dp
private val INDICATOR_STROKE = 3.dp
private val TRIGGER_OFFSET = 72.dp
private val RESTING_OFFSET = 56.dp
private val MAXIMUM_OFFSET = 96.dp
private const val DRAG_RESISTANCE = 0.5f
private const val PARTIAL_ARC_SWEEP = 270f
private const val START_ANGLE = -90f
private const val FULL_TURN_ANGLE = 270f
private const val ROTATION_DURATION = 900
