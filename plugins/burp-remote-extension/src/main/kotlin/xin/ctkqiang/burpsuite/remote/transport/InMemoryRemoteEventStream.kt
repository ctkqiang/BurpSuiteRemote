// 事件流的内存实现。

package xin.ctkqiang.burpsuite.remote.transport

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import xin.ctkqiang.burpsuite.remote.protocol.EventIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope

/**
 * 事件流的内存实现。
 *
 * 只保留最近若干条事件：保留窗口决定「掉线多久之内还能直接续传」，超出窗口的客户端会被要求改取快照（plan §10）。
 * 目前在内存里，扩展重启即归零——这与「事件序号由插件自己产生」的现状一致。
 *
 * 序号与事件身份由本流统一分配：历史、拦截、Repeater 三个发布者各自维护计数器会撞号，
 * 撞号的事件到了客户端会被同步协调器判成重复投递而丢弃——Repeater 条目因此永远进不了投影。
 * 改由这里用一把 AtomicLong 单调递增，所有发布者只填业务字段，序号与身份在落盘前补全。
 */
class InMemoryRemoteEventStream(
    private val maximumRetainedEventCount: Int = DEFAULT_MAXIMUM_RETAINED_EVENT_COUNT,
) : RemoteEventStream {
    private val retainedEvents = ArrayDeque<RemoteEventEnvelope>()

    private val retainedEventsLock = Any()

    // 全局序号分配器：所有发布者共用这一把，appendEvent 时取 next 值。
    private val nextSequenceNumber = AtomicLong(LATEST_SEQUENCE_NUMBER_OF_EMPTY_STREAM)

    // 用 DROP_OLDEST：事件流不阻塞生产者，超出窗口的部分靠保留窗口本身兜底。
    private val publishedEvents =
        MutableSharedFlow<RemoteEventEnvelope>(
            replay = maximumRetainedEventCount,
            extraBufferCapacity = EVENT_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    /**
     * 追加一条事件并广播给当前订阅者。
     *
     * 序号与事件身份在这里补全：调用方传进来的 envelope 上这两个字段会被覆盖，
     * 保证全插件只有一处递增源，历史/拦截/Repeater 三路不会再撞号。
     * 保留窗口满时丢弃最旧的一条：序号连续性由「请求快照」这条路径补齐，而不是靠无限增长的内存。
     */
    fun appendEvent(event: RemoteEventEnvelope) {
        val sequenceNumber = nextSequenceNumber.incrementAndGet()
        val stamped =
            event.copy(
                sequenceNumber = sequenceNumber,
                eventIdentifier = EventIdentifier(EVENT_IDENTIFIER_PREFIX + sequenceNumber),
            )
        synchronized(retainedEventsLock) {
            retainedEvents.addLast(stamped)
            while (retainedEvents.size > maximumRetainedEventCount) {
                retainedEvents.removeFirst()
            }
        }
        publishedEvents.tryEmit(stamped)
    }

    override fun earliestAvailableSequenceNumber(): Long =
        synchronized(retainedEventsLock) {
            retainedEvents.firstOrNull()?.sequenceNumber ?: EARLIEST_SEQUENCE_NUMBER_OF_EMPTY_STREAM
        }

    override fun latestSequenceNumber(): Long = nextSequenceNumber.get()

    override fun observeEvents(): Flow<RemoteEventEnvelope> = publishedEvents.asSharedFlow()

    private companion object {
        // 保留窗口原文未定义，此处选定值：最近 1024 条事件。
        // 依据：手机短暂掉线后能靠它直接续传，而 1024 条只含元数据的事件对插件内存可以忽略。
        private const val DEFAULT_MAXIMUM_RETAINED_EVENT_COUNT = 1_024

        private const val EVENT_BUFFER_CAPACITY = 256

        private const val EARLIEST_SEQUENCE_NUMBER_OF_EMPTY_STREAM = 1L

        private const val LATEST_SEQUENCE_NUMBER_OF_EMPTY_STREAM = 0L

        // 事件身份前缀与序号拼在一起；与历史/拦截发布者原来的写法一致，客户端去重逻辑不用改。
        private const val EVENT_IDENTIFIER_PREFIX = "event_"
    }
}
