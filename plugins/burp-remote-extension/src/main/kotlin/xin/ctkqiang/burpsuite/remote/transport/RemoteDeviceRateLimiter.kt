// 按设备限流的令牌桶。

package xin.ctkqiang.burpsuite.remote.transport

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

/**
 * 按设备计数的令牌桶限流器（rules.md §12 要求 rate limiting）。
 *
 * 每台设备一只桶：一台设备被误触发连点，不应连累另一台正常操作的设备。
 * 时钟由构造器注入，测试因此不必真的等待。
 */
class RemoteDeviceRateLimiter(
    private val clock: Clock,
    private val bucketCapacity: Int = DEFAULT_BUCKET_CAPACITY,
    private val refillTokensPerSecond: Double = DEFAULT_REFILL_TOKENS_PER_SECOND,
) {
    private val tokenBuckets = ConcurrentHashMap<DeviceIdentifier, TokenBucket>()

    /**
     * 为某台设备取一个令牌。
     *
     * 返回 false 表示该设备已超出速率限制，调用方应回以「已限流」而不是执行命令。
     */
    fun tryAcquireToken(deviceIdentifier: DeviceIdentifier): Boolean {
        val tokenBucket =
            tokenBuckets.computeIfAbsent(deviceIdentifier) {
                TokenBucket(
                    capacity = bucketCapacity.toDouble(),
                    initialTokens = bucketCapacity.toDouble(),
                    initialMillis = clock.millis(),
                )
            }
        return synchronized(tokenBucket) {
            tokenBucket.refillTo(clock.millis(), refillTokensPerSecond)
            tokenBucket.tryConsumeToken()
        }
    }

    private companion object {
        // 限流速率原文未定义，此处选定值：突发 20 条、稳态每秒 5 条。
        // 依据：人工在手机上操作拦截项远达不到每秒 5 次，而误触连点又不会立刻被挡住。
        private const val DEFAULT_BUCKET_CAPACITY = 20

        private const val DEFAULT_REFILL_TOKENS_PER_SECOND = 5.0
    }
}

// 桶状态只被持有它的锁保护，因此内部不加同步。
private class TokenBucket(
    private val capacity: Double,
    initialTokens: Double,
    initialMillis: Long,
) {
    private var availableTokens = initialTokens

    private var lastRefillAtMillis = initialMillis

    fun refillTo(
        currentMillis: Long,
        refillTokensPerSecond: Double,
    ) {
        val elapsedMillis = currentMillis - lastRefillAtMillis
        if (elapsedMillis <= NO_ELAPSED_MILLISECONDS) {
            return
        }
        lastRefillAtMillis = currentMillis
        val refilledTokens = elapsedMillis.toDouble() / MILLISECONDS_PER_SECOND * refillTokensPerSecond
        availableTokens = (availableTokens + refilledTokens).coerceAtMost(capacity)
    }

    fun tryConsumeToken(): Boolean {
        if (availableTokens < TOKEN_COST_PER_COMMAND) {
            return false
        }
        availableTokens -= TOKEN_COST_PER_COMMAND
        return true
    }

    private companion object {
        private const val MILLISECONDS_PER_SECOND = 1_000.0

        private const val TOKEN_COST_PER_COMMAND = 1.0

        private const val NO_ELAPSED_MILLISECONDS = 0L
    }
}
