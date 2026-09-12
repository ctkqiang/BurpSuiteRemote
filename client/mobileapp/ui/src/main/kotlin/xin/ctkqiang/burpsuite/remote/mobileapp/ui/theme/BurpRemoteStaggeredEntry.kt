package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 列表项的错峰入场。
 *
 * 错峰让一列同级信息按阅读顺序落下，用户因此看得出哪些是同一批；纯位移没有信息量，
 * 所以这里只做很短的位移加上淡入（rules.md §8.3 之外的动效约定见 plan §45）。
 */
@Composable
fun Modifier.burpRemoteStaggeredEntry(index: Int): Modifier {
    val entryProgress = remember { Animatable(START_PROGRESS) }

    LaunchedEffect(index) {
        entryProgress.animateTo(
            targetValue = END_PROGRESS,
            animationSpec =
                tween(
                    durationMillis = BurpRemoteMotion.DURATION_REGULAR,
                    delayMillis = staggerDelayFor(index),
                    easing = BurpRemoteMotion.EasingStandard,
                ),
        )
    }

    return graphicsLayer {
        alpha = entryProgress.value
        translationY = (END_PROGRESS - entryProgress.value) * ENTRY_TRANSLATION_PIXELS
    }
}

// 前几项才错峰：一屏之外的项等它滚到眼前时早就该画好了，继续延迟只会让它看起来像卡住。
private fun staggerDelayFor(index: Int): Int =
    if (index >= MAXIMUM_STAGGERED_INDEX) {
        MAXIMUM_STAGGER_DELAY_MILLISECONDS
    } else {
        index * STAGGER_STEP_MILLISECONDS
    }

private const val START_PROGRESS = 0f
private const val END_PROGRESS = 1f
private const val ENTRY_TRANSLATION_PIXELS = 24f
private const val STAGGER_STEP_MILLISECONDS = 32
private const val MAXIMUM_STAGGERED_INDEX = 6
private const val MAXIMUM_STAGGER_DELAY_MILLISECONDS = MAXIMUM_STAGGERED_INDEX * STAGGER_STEP_MILLISECONDS
