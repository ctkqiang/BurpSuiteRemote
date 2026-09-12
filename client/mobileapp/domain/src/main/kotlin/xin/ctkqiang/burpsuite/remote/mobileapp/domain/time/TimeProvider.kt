package xin.ctkqiang.burpsuite.remote.mobileapp.domain.time

import java.time.Instant

/**
 * 时间来源。
 *
 * 时间必须注入：测试一旦读墙上时钟就不再确定（rules.md §13），而「收到时刻」「校验点时刻」这
 * 类字段又必须有人提供。
 */
fun interface TimeProvider {
    /** 当前时刻。 */
    fun now(): Instant
}
