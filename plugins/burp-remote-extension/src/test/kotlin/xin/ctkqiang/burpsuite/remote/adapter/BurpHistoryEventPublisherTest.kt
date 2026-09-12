/**
 * 事件发布测试：事件只在 Burp 真的产生条目时发布，同一条目不重复，卸载后摘掉订阅并不再触碰 Burp。
 */

package xin.ctkqiang.burpsuite.remote.adapter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.AggregateType
import xin.ctkqiang.burpsuite.remote.transport.InMemoryRemoteEventStream
import java.time.Instant

// 周期扫描只能在虚拟时间上验证：advanceTimeBy 与 runCurrent 仍是实验 API，这里显式接受它。
@OptIn(ExperimentalCoroutinesApi::class)
class BurpHistoryEventPublisherTest {
    @Test
    fun `nothing is published while burp history stays empty`() {
        runTest {
            val fixture = createFixture(backgroundScope)

            fixture.publisher.start()
            fixture.signalSource.fireSignal()
            advanceTimeBy(SWEEP_INTERVAL_MILLISECONDS * SWEEP_TICKS)
            runCurrent()

            assertEquals(0L, fixture.eventStream.latestSequenceNumber())
            fixture.publisher.stop()
        }
    }

    @Test
    fun `an entry that existed before start is served by the rest endpoint and not replayed as an event`() {
        runTest {
            val fixture = createFixture(backgroundScope)
            fixture.historySource.appendEntry(StubProxyHistoryEntry())

            fixture.publisher.start()
            fixture.signalSource.fireSignal()
            advanceTimeBy(SWEEP_INTERVAL_MILLISECONDS * SWEEP_TICKS)
            runCurrent()

            assertEquals(0L, fixture.eventStream.latestSequenceNumber())
            fixture.publisher.stop()
        }
    }

    @Test
    fun `an entry burp produces after start is published with the metadata of that entry`() {
        runTest {
            val fixture = createFixture(backgroundScope)
            val stubEntry = StubProxyHistoryEntry()
            fixture.publisher.start()

            fixture.historySource.appendEntry(stubEntry)
            fixture.signalSource.fireSignal()
            val observedEvent = fixture.eventStream.observeEvents().take(1).toList().single()

            assertEquals("history.item.observed", observedEvent.eventType)
            assertEquals(AggregateType.History, observedEvent.aggregateType)
            assertEquals(
                fixture.historyAdapter.toHistoryIdentifier(stubEntry).value,
                observedEvent.aggregateIdentifier.value,
            )
            assertEquals(FIRST_SEQUENCE_NUMBER, observedEvent.sequenceNumber)
            assertEquals(Instant.ofEpochMilli(stubEntry.occurredAtEpochMilliseconds), observedEvent.occurredAt)
            assertEquals(StubProxyHistoryEntry.DEFAULT_METHOD, observedEvent.payload.metadataTextAt("method"))
            assertEquals(StubProxyHistoryEntry.DEFAULT_PATH, observedEvent.payload.metadataTextAt("path"))
            fixture.publisher.stop()
        }
    }

    @Test
    fun `entries are published once each and in the order burp lists them`() {
        runTest {
            val fixture = createFixture(backgroundScope)
            val firstEntry = StubProxyHistoryEntry(path = "/api/first")
            val secondEntry = StubProxyHistoryEntry(path = "/api/second")
            fixture.publisher.start()

            fixture.historySource.appendEntry(firstEntry)
            fixture.historySource.appendEntry(secondEntry)
            fixture.signalSource.fireSignal()
            val observedEvents = fixture.eventStream.observeEvents().take(2).toList()

            assertEquals(
                listOf(FIRST_SEQUENCE_NUMBER, SECOND_SEQUENCE_NUMBER),
                observedEvents.map { it.sequenceNumber },
            )
            assertEquals(
                listOf(
                    fixture.historyAdapter.toHistoryIdentifier(firstEntry).value,
                    fixture.historyAdapter.toHistoryIdentifier(secondEntry).value,
                ),
                observedEvents.map { it.aggregateIdentifier.value },
            )
            fixture.publisher.stop()
        }
    }

    @Test
    fun `repeated signals and sweeps do not publish an entry twice`() {
        runTest {
            val fixture = createFixture(backgroundScope)
            fixture.publisher.start()
            fixture.historySource.appendEntry(StubProxyHistoryEntry())

            fixture.signalSource.fireSignal()
            fixture.signalSource.fireSignal()
            advanceTimeBy(SWEEP_INTERVAL_MILLISECONDS * SWEEP_TICKS)
            runCurrent()

            assertEquals(FIRST_SEQUENCE_NUMBER, fixture.eventStream.latestSequenceNumber())
            fixture.publisher.stop()
        }
    }

    @Test
    fun `the periodic sweep publishes an entry even when no signal arrives`() {
        runTest {
            val fixture = createFixture(backgroundScope)
            fixture.publisher.start()

            fixture.historySource.appendEntry(StubProxyHistoryEntry())
            advanceTimeBy(SWEEP_INTERVAL_MILLISECONDS + 1)
            runCurrent()

            assertEquals(FIRST_SEQUENCE_NUMBER, fixture.eventStream.latestSequenceNumber())
            fixture.publisher.stop()
        }
    }

    @Test
    fun `a cleared history publishes nothing and resumes from the new watermark`() {
        runTest {
            val fixture = createFixture(backgroundScope)
            fixture.historySource.appendEntry(StubProxyHistoryEntry(path = "/api/before-clear"))
            fixture.publisher.start()

            fixture.historySource.clearEntries()
            fixture.signalSource.fireSignal()
            assertEquals(0L, fixture.eventStream.latestSequenceNumber())

            val entryAfterClear = StubProxyHistoryEntry(path = "/api/after-clear")
            fixture.historySource.appendEntry(entryAfterClear)
            fixture.signalSource.fireSignal()
            val observedEvent = fixture.eventStream.observeEvents().take(1).toList().single()

            assertEquals(
                fixture.historyAdapter.toHistoryIdentifier(entryAfterClear).value,
                observedEvent.aggregateIdentifier.value,
            )
            fixture.publisher.stop()
        }
    }

    @Test
    fun `stopping cancels the subscription and stops touching burp`() {
        runTest {
            val fixture = createFixture(backgroundScope)
            fixture.publisher.start()

            fixture.publisher.stop()

            assertTrue(fixture.signalSource.isSubscriptionCancelled)
            val readCountAfterStop = fixture.historySource.readCount
            fixture.signalSource.fireSignal()
            advanceTimeBy(SWEEP_INTERVAL_MILLISECONDS * SWEEP_TICKS)
            runCurrent()
            assertEquals(readCountAfterStop, fixture.historySource.readCount)
        }
    }

    private fun createFixture(sweepScope: CoroutineScope): PublisherFixture = PublisherFixture(sweepScope)

    private fun JsonElement.metadataTextAt(fieldName: String): String? = jsonObject[fieldName]?.jsonPrimitive?.content

    private class PublisherFixture(sweepScope: CoroutineScope) {
        val historySource = StubProxyHistorySource()

        val signalSource = StubProxyHistorySignalSource()

        val eventStream = InMemoryRemoteEventStream()

        val historyAdapter = BurpHistoryAdapter(historySource)

        val publisher =
            BurpHistoryEventPublisher(
                historyAdapter = historyAdapter,
                historySignalSource = signalSource,
                eventStream = eventStream,
                sweepScope = sweepScope,
                sweepIntervalMilliseconds = SWEEP_INTERVAL_MILLISECONDS,
            )
    }

    private companion object {
        private const val SWEEP_INTERVAL_MILLISECONDS = 100L

        private const val SWEEP_TICKS = 3

        private const val FIRST_SEQUENCE_NUMBER = 1L

        private const val SECOND_SEQUENCE_NUMBER = 2L
    }
}
