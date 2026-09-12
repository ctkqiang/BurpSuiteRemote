package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

import kotlin.math.min
import kotlin.math.pow

/**
 * 重连退避策略。
 *
 * 原文未定义数值，这里取「有上限的指数退避」：掉线后立刻重试会让手机与插件一起空转，
 * 退避到看不见的上限又会让用户以为应用已经死了。
 */
data class ReconnectionPolicy(
    /** 第一次重连前的等待。 */
    val initialDelayMilliseconds: Long = 1_000L,
    /** 等待的上限，之后一直是这个值。 */
    val maximumDelayMilliseconds: Long = 30_000L,
    /** 每次重连等待的倍增系数。 */
    val delayMultiplier: Double = 2.0,
) {
    /** 第 attemptNumber 次重连前该等多久；attemptNumber 从 1 开始，越界一律按上限处理。 */
    fun delayBeforeAttempt(attemptNumber: Int): Long {
        if (attemptNumber <= 1) return initialDelayMilliseconds

        val exponentialDelay = initialDelayMilliseconds.toDouble() * delayMultiplier.pow(attemptNumber - 1)
        val boundedDelay = min(exponentialDelay, maximumDelayMilliseconds.toDouble()).toLong()

        return boundedDelay.coerceAtLeast(initialDelayMilliseconds)
    }
}
