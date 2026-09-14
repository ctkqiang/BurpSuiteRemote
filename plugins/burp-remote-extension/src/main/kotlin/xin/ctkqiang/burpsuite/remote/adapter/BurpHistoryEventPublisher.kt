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
 *
 * 每个历史 identifier 不只发一次事件：Burp 里同一条 entry 可能先只带请求字段就进入代理历史
 * （statusCode=null、responseLength=null、duration=null），等响应到齐后这些响应端字段才补齐。
 * publisher 每次 reconcile 会对所有当前 Burp 里的 identifier 重新算响应端字段摘要，
 * 摘要变了就再发一条 observed —— 客户端 reducer 只要 eventIdentifier 递增就会覆盖旧值。
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

    // 已见过的 identifier 到"响应端字段摘要"的映射；摘要变了就重发 observed 事件。
    // 只放 Burp 当前历史里的 identifier，历史被清空时旧条目自然不在里面。
    private val knownEntries = LinkedHashMap<String, String>()

    private var signalSubscription: BurpHistorySignalSubscription? = null

    private var sweepJob: Job? = null

    private var isRunning = false

    /**
     * 订阅历史信号并启动兜底扫描。
     *
     * 启动时先把水位对齐到现有历史：存量条目的响应端摘要都算一遍，但**不发事件**——
     * 事件流只承担其后的变化，启动时就把 Burp 里已有的全量发一遍会让客户端被历史淹没。
     */
    fun start() {
        synchronized(publicationLock) {
            if (isRunning) {
                return
            }
            nextSequenceNumber.set(eventStream.latestSequenceNumber())
            // 把 Burp 当前所有条目记入 knownEntries，但不发事件：这些存量条目的事件客户端
            // 应该从别的地方（比如 REST /v1/history）同步，事件流只负责增量。
            historyAdapter.readHistoryEntries().forEach { entry ->
                val id = historyAdapter.toHistoryIdentifier(entry).value
                knownEntries[id] = responseFingerprint(entry)
            }
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

    /** 对照 Burp 历史发布尚未发布过或已变化的条目。 */
    fun reconcileWithBurpHistory() {
        synchronized(publicationLock) {
            if (!isRunning) {
                return
            }
            val currentEntries = historyAdapter.readHistoryEntries()
            val currentByIdentifier =
                LinkedHashMap<String, BurpProxyHistoryEntry>(currentEntries.size).apply {
                    currentEntries.forEach { entry ->
                        put(historyAdapter.toHistoryIdentifier(entry).value, entry)
                    }
                }

            // 对每个当前 identifier：新的 → 发 observed；已知但响应端字段变了 → 也发 observed。
            for ((identifier, entry) in currentByIdentifier) {
                val currentFingerprint = responseFingerprint(entry)
                val previousFingerprint = knownEntries[identifier]
                if (previousFingerprint != currentFingerprint) {
                    publishHistoryItemObserved(entry)
                    knownEntries[identifier] = currentFingerprint
                }
            }

            // 已知但 Burp 里已经不存在的 identifier → 从 knownEntries 移除；历史被清空时也能正确跟进。
            val currentIdentifiers = currentByIdentifier.keys
            knownEntries.keys.retainAll { it in currentIdentifiers }
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

    /**
     * 响应端字段的摘要；同一 entry 的响应端字段没变时摘要不变，任何一个响应端字段变了摘要就变。
     * 用 NUL 分隔，和 identifier 同样的规则避免跨字段撞车。
     */
    private fun responseFingerprint(entry: BurpProxyHistoryEntry): String =
        listOf(
            entry.statusCode?.toString() ?: "null",
            entry.mimeTypeText,
            entry.responseLength?.toString() ?: "null",
            entry.durationMilliseconds?.toString() ?: "null",
            entry.destinationInternetProtocolAddress ?: "null",
            entry.isSecure.toString(),
            entry.listenerPort.toString(),
        ).joinToString(IDENTIFIER_FIELD_SEPARATOR)

    private companion object {
        // 钩子可能早于历史落库，所以信号之外还要定期兜底；秒级延迟对手机端可以忽略。
        private const val DEFAULT_SWEEP_INTERVAL_MILLISECONDS = 2_000L

        private const val HISTORY_ITEM_OBSERVED_EVENT_TYPE = "history.item.observed"

        private const val EVENT_IDENTIFIER_PREFIX = "event_"

        private const val IDENTIFIER_FIELD_SEPARATOR = "\u0000"
    }
}
