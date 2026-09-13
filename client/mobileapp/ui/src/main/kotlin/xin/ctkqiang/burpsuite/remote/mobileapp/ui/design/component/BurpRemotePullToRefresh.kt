package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 下拉刷新。
 *
 * 自己实现而不是取 Material 3 的那一个：设计系统不引 Material 3，而刷新指示器是第一个必须换掉的东西。
 * 指示器画在内容之上、内容整体下移，因此不存在「把列表项压扁」的中间态。
 *
 * 位移用一个普通的状态保存，而不是 `Animatable`：`Animatable` 自带互斥锁，后一次操作会取消前一次。
 * 拖动的位移由滚动回调写（非挂起、每帧都发生），回弹由另一个协程动画写，两者一旦撞上，
 * 回弹动画就会被取消 —— 而触发刷新恰恰写在回弹动画之后，于是「拉下去、指示器停在半路、刷新没发生」。
 * 普通状态没有锁，写入是同步的；回弹则收敛到唯一一个协程入口，并在开始前取消上一次。
 */
@Composable
fun BurpRemotePullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val density = LocalDensity.current
    val settleScope = rememberCoroutineScope()
    val pullOffsetPixels = remember { mutableFloatStateOf(0f) }
    val settleJob = remember { mutableStateOf<Job?>(null) }
    // 这两个用 rememberUpdatedState 包住，下面那个 NestedScrollConnection 才能只建一次：
    // 手势进行到一半换掉它，滚动分发会拿不准该把余下的位移交给谁。
    val isRefreshingState = rememberUpdatedState(isRefreshing)
    val onRefreshState = rememberUpdatedState(onRefresh)
    val triggerOffsetPixels = with(density) { TRIGGER_OFFSET.toPx() }
    val restingOffsetPixels = with(density) { RESTING_OFFSET.toPx() }
    val maximumOffsetPixels = with(density) { MAXIMUM_OFFSET.toPx() }

    // 回弹与收回都走这一个入口：先取消上一次，再动画到目标值。
    val settleTo: (Float, Int) -> Unit =
        remember(settleScope) {
            { targetOffsetPixels, durationMillis ->
                settleJob.value?.cancel()
                settleJob.value =
                    settleScope.launch {
                        animate(
                            initialValue = pullOffsetPixels.floatValue,
                            targetValue = targetOffsetPixels,
                            animationSpec =
                                tween(durationMillis, easing = BurpRemoteMotion.EasingStandard),
                        ) { currentOffsetPixels, _ -> pullOffsetPixels.floatValue = currentOffsetPixels }
                    }
            }
        }

    // 刷新结束就把指示器收回去；不回头收的话它会在下一次下拉之前一直占着位置。
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) settleTo(0f, BurpRemoteMotion.DURATION_REGULAR)
    }

    val nestedScrollConnection =
        remember(settleTo, triggerOffsetPixels, restingOffsetPixels, maximumOffsetPixels) {
            object : NestedScrollConnection {
                // 只有内容已经到顶、还在继续往下拉时才算下拉；否则会跟列表自身的滚动打架。
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (isRefreshingState.value || available.y <= 0f) return Offset.Zero
                    // 手指重新抓住把手：正在进行的回弹立刻停下，位移改由手指决定。
                    settleJob.value?.cancel()
                    pullOffsetPixels.floatValue =
                        (pullOffsetPixels.floatValue + available.y * DRAG_RESISTANCE)
                            .coerceIn(0f, maximumOffsetPixels)
                    return Offset(x = 0f, y = available.y)
                }

                // 反向滑动先把下拉位移收回去，别让用户以为列表卡住了。
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    val currentOffsetPixels = pullOffsetPixels.floatValue
                    if (available.y >= 0f || currentOffsetPixels <= 0f) return Offset.Zero
                    settleJob.value?.cancel()
                    val consumedY = available.y.coerceAtLeast(-currentOffsetPixels)
                    pullOffsetPixels.floatValue = (currentOffsetPixels + consumedY).coerceAtLeast(0f)
                    return Offset(x = 0f, y = consumedY)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pullOffsetPixels.floatValue >= triggerOffsetPixels) {
                        // 先把刷新发出去，再让指示器回到停靠位。顺序反过来的话，
                        // 回弹一旦被手指重新抓住而取消，这次刷新就跟着一起没了。
                        onRefreshState.value()
                        settleTo(restingOffsetPixels, BurpRemoteMotion.DURATION_FAST)
                    } else {
                        settleTo(0f, BurpRemoteMotion.DURATION_REGULAR)
                    }
                    return Velocity.Zero
                }
            }
        }

    // 下拉进度只驱动指示器；内容整体下移走的 graphicsLayer，因此拖动期间不会重建列表布局。
    val pullProgress by
        produceState(initialValue = 0f, pullOffsetPixels, triggerOffsetPixels) {
            snapshotFlow { (pullOffsetPixels.floatValue / triggerOffsetPixels).coerceIn(0f, 1f) }
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
                    .graphicsLayer { translationY = pullOffsetPixels.floatValue },
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
