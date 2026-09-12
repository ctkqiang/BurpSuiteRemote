package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * 动效的时长与缓动。
 *
 * 全项目只有这三个时长与一条缓动：每屏各挑一个时长，动效就会从「提示层级」变成「各跳各的」。
 */
object BurpRemoteMotion {
    /** 120ms：点击反馈这类必须被感知成即时的高频动作。 */
    const val DURATION_FAST = 120

    /** 220ms：常规出现、消失与状态切换。 */
    const val DURATION_REGULAR = 220

    /** 320ms：强调，例如浮底栏这类需要被看清的进场。 */
    const val DURATION_EMPHASISED = 320

    /** 标准缓动：快出慢入，位移看起来才像是被手带过去的。 */
    val EasingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
