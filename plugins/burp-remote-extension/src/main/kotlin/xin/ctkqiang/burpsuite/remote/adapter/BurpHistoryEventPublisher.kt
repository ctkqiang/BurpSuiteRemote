// 把 Burp 真实产生的历史条目发布成事件。

package xin.ctkqiang.burpsuite.remote.adapter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.protocol.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.AggregateType
import xin.ctkqiang.burpsuite.remote.protocol.EventIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.transport.InMemoryRemoteEventStream
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/**
 * 观察 Burp 代理历史，为每条真实产生的记录发布一条 `history.item.observed` 事件（plan §82、§83）。
 *
 * 信号与周期扫描都只是「去看一眼」的触发，事件内容一律来自 Burp 自己的历史条目：不编造、不预告、不补发。
 */
class BurpHistoryEventPublisher(
    private val historyAdapter: BurpHistoryAdapter,
    private val historySignalSource: BurpProxyHistorySignalSource,
    private val eventStream: InMemoryRemoteEventStream,
    private val sweepScope: CoroutineScope,
    private val sweepIntervalMilliseconds: Long = DEFAULT_SWEEP_INTERVAL_MILLISECONDS,
) {
    private val publicationLock = Any()

    // 序号只在这里产生：事件日志的单调性由它保证。
    private val nextSequenceNumber = AtomicLong()

    private var observedEntryCount = 0

    private var signalSubscription: BurpHistorySignalSubscription? = null

    private var sweepJob: Job? = null

    private var isRunning = false

    /**
     * 订阅历史信号并启动兜底扫描。
     *
     * 启动时先把水位对齐到现有历史：存量条目由 `GET /v1/history` 供给，事件流只承担其后的增量，客户端重连时不会被历史淹没。
     */
    fun start() {
        synchronized(publicationLock) {
            if (isRunning) {
                return
            }
            nextSequenceNumber.set(eventStream.latestSequenceNumber())
            observedEntryCount = historyAdapter.readHistoryEntries().size
            isRunning = true
            signalSubscription = historySignalSource.subscribeToHistorySignals(::reconcileWithBurpHistory)
            sweepJob =
                sweepScope.launch {
                    while (true) {
                        delay(sweepIntervalMilliseconds)
                        reconcileWithBurpHistory()
                    }
                }
        }
    }

    /**
     * 取消订阅并停掉兜底扫描。
     *
     * 卸载之后不得再触碰 Burp API，所以这里同步摘掉回调并结束扫描协程，而不是留给协程自己发现。
     */
    fun stop() {
        synchronized(publicationLock) {
            isRunning = false
            signalSubscription?.cancel()
            signalSubscription = null
            sweepJob?.cancel()
            sweepJob = null
        }
    }

    /** 对照 Burp 历史发布尚未发布过的条目；同一条目只发布一次。 */
    fun reconcileWithBurpHistory() {
        synchronized(publicationLock) {
            if (!isRunning) {
                return
            }
            val entries = historyAdapter.readHistoryEntries()
            // 历史被清空或没有增长时只对齐水位：为了列表好看而补发事件就是编造事实。
            if (entries.size <= observedEntryCount) {
                observedEntryCount = entries.size
                return
            }
            for (entryIndex in observedEntryCount until entries.size) {
                publishHistoryItemObserved(entries[entryIndex])
            }
            observedEntryCount = entries.size
        }
    }

    private fun publishHistoryItemObserved(entry: BurpProxyHistoryEntry) {
        val historyIdentifier = historyAdapter.toHistoryIdentifier(entry)
        val sequenceNumber = nextSequenceNumber.incrementAndGet()
        eventStream.appendEvent(
            RemoteEventEnvelope(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                eventIdentifier = EventIdentifier(EVENT_IDENTIFIER_PREFIX + sequenceNumber),
                sequenceNumber = sequenceNumber,
                // 事实发生时刻来自 Burp，而不是这里读到它的时刻（rules.md §4.5）。
                occurredAt = Instant.ofEpochMilli(entry.occurredAtEpochMilliseconds),
                eventType = HISTORY_ITEM_OBSERVED_EVENT_TYPE,
                aggregateType = AggregateType.History,
                aggregateIdentifier = AggregateIdentifier(historyIdentifier.value),
                payload = historyAdapter.buildMetadataPayload(entry),
            ),
        )
    }

    private companion object {
        // 钩子可能早于历史落库，所以信号之外还要定期兜底；秒级延迟对手机端可以忽略。
        private const val DEFAULT_SWEEP_INTERVAL_MILLISECONDS = 2_000L

        private const val HISTORY_ITEM_OBSERVED_EVENT_TYPE = "history.item.observed"

        private const val EVENT_IDENTIFIER_PREFIX = "event_"
    }
}
