package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 列表项的错峰入场。
 *
 * 错峰让一列同级信息按阅读顺序落下，用户因此看得出哪些是同一批；纯位移没有信息量，
 * 所以这里只做很短的位移加上淡入。
 */
@Composable
fun Modifier.burpRemoteStaggeredEntry(index: Int): Modifier {
    val entryProgress = remember { Animatable(START_PROGRESS) }
    val entryTranslationPixels = with(LocalDensity.current) { ENTRY_TRANSLATION.toPx() }

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
        translationY = (END_PROGRESS - entryProgress.value) * entryTranslationPixels
    }
}

// 前几项才错峰：一屏之外的项等它滚到眼前时早就该画好了，继续延迟只会让它看起来像卡住。
private fun staggerDelayFor(index: Int): Int =
    if (index >= MAXIMUM_STAGGERED_ITEM_COUNT) {
        MAXIMUM_STAGGERED_ITEM_COUNT * STAGGER_STEP_MILLISECONDS
    } else {
        index * STAGGER_STEP_MILLISECONDS
    }

private val ENTRY_TRANSLATION = 8.dp
private const val STAGGER_STEP_MILLISECONDS = 30
private const val MAXIMUM_STAGGERED_ITEM_COUNT = 8
private const val START_PROGRESS = 0f
private const val END_PROGRESS = 1f
