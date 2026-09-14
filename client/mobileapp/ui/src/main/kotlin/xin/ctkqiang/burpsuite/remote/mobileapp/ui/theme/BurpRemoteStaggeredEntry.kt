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
 *
 * 只在首批项上生效：懒加载列表里，一行是滚进视口那一刻才组合的，若每一行都淡一次，
 * 用户滚动时会看到一排接一排地闪——那些行本来就该是已经画好的。首屏之内才值得入场。
 */
@Composable
fun Modifier.burpRemoteStaggeredEntry(index: Int): Modifier {
    if (index >= MAXIMUM_STAGGERED_ITEM_COUNT) {
        return this
    }

    val entryProgress = remember { Animatable(START_PROGRESS) }
    val entryTranslationPixels = with(LocalDensity.current) { ENTRY_TRANSLATION.toPx() }

    LaunchedEffect(index) {
        entryProgress.animateTo(
            targetValue = END_PROGRESS,
            animationSpec =
                tween(
                    durationMillis = BurpRemoteMotion.DURATION_REGULAR,
                    delayMillis = index * STAGGER_STEP_MILLISECONDS,
                    easing = BurpRemoteMotion.EasingStandard,
                ),
        )
    }

    return graphicsLayer {
        alpha = entryProgress.value
        translationY = (END_PROGRESS - entryProgress.value) * entryTranslationPixels
    }
}

// 首屏大约能看到这么多行；再多就属于「滚进来时早该画好」的那一批。
private const val MAXIMUM_STAGGERED_ITEM_COUNT = 8

// 一屏之内的项最多等 7 档，仍在一瞬之内；超过这个数的项不再入场。
private const val STAGGER_STEP_MILLISECONDS = 30

private val ENTRY_TRANSLATION = 8.dp
private const val START_PROGRESS = 0f
private const val END_PROGRESS = 1f
