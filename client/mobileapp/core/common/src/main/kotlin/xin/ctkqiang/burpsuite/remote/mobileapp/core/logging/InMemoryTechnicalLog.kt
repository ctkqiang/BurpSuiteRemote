// 在内存里留存最近若干条日志的实现。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * 日志面板要读的一行：日志本身，加上「第几条」与「什么时候记的」。
 *
 * 时刻与序号由存储侧盖章，而不是由调用方填：一条日志的产生时刻就是它被记下的时刻，
 * 让三十多个调用点各自取一次时钟，只会得到三十多种互不一致的写法。
 *
 * @property sequence 单调递增的序号，从 1 开始；排序读它，不依赖时钟的分辨率。
 * @property recordedAt 这条日志被记下的时刻。
 * @property event 日志内容。
 */
data class TechnicalLogEntry(
    val sequence: Long,
    val recordedAt: Instant,
    val event: TechnicalLogEvent,
)

/**
 * 既落地、又在内存里留一份最近日志的技术日志。
 *
 * 界面上的日志面板要看到刚发生的事，而 logcat 隔着一条 adb 才够得着；这里把最近
 * [capacity] 条留在进程里，界面直接读这一份，两条出口写的是同一个事实。
 *
 * 保留上限是硬性的：日志是一条无限流，不封顶就是一个稳定的内存泄漏，一次跑一整天的会话
 * 会把它撑到几百兆。超出上限时丢最旧的，新的一律留下。
 *
 * @param delegate 真正的落地实现；默认什么都不做，便于测试与不需要 logcat 的场景。
 * @param clock 时间源；注入而不是直接读墙上时钟，测试才能断言序号与时刻（rules.md §13）。
 * @param capacity 内存里最多保留多少条；必须为正数。
 */
class InMemoryTechnicalLog(
    private val delegate: TechnicalLog = SilentTechnicalLog,
    private val clock: () -> Instant = Instant::now,
    private val capacity: Int = DEFAULT_CAPACITY,
) : TechnicalLog {
    private val retainedEntries = MutableStateFlow<List<TechnicalLogEntry>>(emptyList())

    /** 最近若干条日志，最旧在前；`record` 从任意线程调用都安全。 */
    val entries: StateFlow<List<TechnicalLogEntry>> = retainedEntries.asStateFlow()

    init {
        require(capacity > 0) { "日志的保留上限必须为正数，否则每一条都会被静默丢掉" }
    }

    /** 先落地再留档：落地是事实来源，留档只是它的一份可读副本。 */
    override fun record(event: TechnicalLogEvent) {
        delegate.record(event)
        val recordedAt = clock()
        retainedEntries.update { current ->
            val nextSequence = current.lastOrNull()?.sequence?.plus(1) ?: FIRST_SEQUENCE
            (current + TechnicalLogEntry(sequence = nextSequence, recordedAt = recordedAt, event = event))
                .takeLast(capacity)
        }
    }

    private companion object {
        /** 默认保留条数：够翻完一次配对加一轮请求，又不至于长期占着内存。 */
        const val DEFAULT_CAPACITY = 500

        const val FIRST_SEQUENCE = 1L
    }
}
