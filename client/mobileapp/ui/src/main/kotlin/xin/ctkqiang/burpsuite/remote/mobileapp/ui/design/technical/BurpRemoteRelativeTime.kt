// 「多久之前」的展示文本。

package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import java.time.Duration
import java.time.Instant

/**
 * 把两个时刻的差写成「多久之前」。
 *
 * 相对时间只回答「多久」，绝对时刻由调用方另给：扫视时要的是前者，写进报告时要的是后者，
 * 挤在同一处会让两边都不好用。这里也是全应用唯一一处相对时间排版，各屏不再各写一份。
 *
 * @param now 当前时刻；由状态里的时钟给，不在这里读墙上时钟（rules.md §13）。
 * @param moment 要比较的时刻。
 * @return 例如「3 分钟前」；`moment` 在未来时按「刚刚」处理，不显示负数。
 */
@Composable
fun burpRemoteRelativeTimeText(
    now: Instant,
    moment: Instant,
): String {
    val resources = LocalContext.current.resources
    val elapsedSeconds = Duration.between(moment, now).seconds.coerceAtLeast(0L)
    return when {
        elapsedSeconds < JUST_NOW_SECONDS -> resources.getString(R.string.components_relative_just_now)

        elapsedSeconds < SECONDS_PER_MINUTE ->
            resources.quantityText(R.plurals.components_relative_seconds, elapsedSeconds)

        elapsedSeconds < SECONDS_PER_HOUR ->
            resources.quantityText(R.plurals.components_relative_minutes, elapsedSeconds / SECONDS_PER_MINUTE)

        elapsedSeconds < SECONDS_PER_DAY ->
            resources.quantityText(R.plurals.components_relative_hours, elapsedSeconds / SECONDS_PER_HOUR)

        else ->
            resources.quantityText(R.plurals.components_relative_days, elapsedSeconds / SECONDS_PER_DAY)
    }
}

/**
 * 取与数量匹配的复数文案。
 *
 * `getQuantityString` 的 `count` 参数是 Int，格式化参数是可变参数；两者都要给，
 * 少给一个在只有单数形式的语言里看不出问题，到德语里就会显示出占位符。
 *
 * @param pluralResource 复数资源 id。
 * @param value 参与复数选择的数量。
 * @return 与数量匹配的文案。
 */
private fun Resources.quantityText(
    pluralResource: Int,
    value: Long,
): String = getQuantityString(pluralResource, value.toInt(), value.toInt())

// 十秒以内一律说「刚刚」：秒级的跳动对读的人没有信息量，只会让列表一直在动。
private const val JUST_NOW_SECONDS = 10L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_DAY = 86_400L
