/**
 * 事件通道测试：未认证不收事件、认证后按序号续传、区间不可用时要求快照、未配对设备认证失败。
 * 用真实 CIO 端口 + JDK 内置 WebSocket 客户端，因为仓库现有依赖里没有 Ktor 客户端 WebSocket 插件。
 */

package xin.ctkqiang.burpsuite.remote.transport

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.AggregateType
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.EventIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RemoteClientMessage
import xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope
import xin.ctkqiang.burpsuite.remote.protocol.RemoteMessageType
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolJson
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.security.PairedDeviceRegistry
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class RemoteWebSocketServerTest {
    @Test
    fun `an unauthenticated session receives no events`() {
        val eventStream = InMemoryRemoteEventStream()
        eventStream.appendEvent(event(sequenceNumber = FIRST_SEQUENCE_NUMBER))

        withRunningEventServer(eventStream) { port ->
            val probe = JdkWebSocketProbe.connect(port)
            try {
                probe.sendText(connectRequestText())

                assertNull(probe.pollMessage(RECEIVE_TIMEOUT_MILLISECONDS))
            } finally {
                probe.close()
            }
        }
    }

    @Test
    fun `an authenticated session replays retained events after a resume`() {
        val eventStream = InMemoryRemoteEventStream()
        eventStream.appendEvent(event(sequenceNumber = FIRST_SEQUENCE_NUMBER))
        eventStream.appendEvent(event(sequenceNumber = SECOND_SEQUENCE_NUMBER))

        withRunningEventServer(eventStream) { port ->
            val probe = JdkWebSocketProbe.connect(port)
            try {
                probe.sendText(connectRequestText())
                probe.sendText(authenticateRequestText(PAIRED_DEVICE_IDENTIFIER.value))
                assertEquals(AUTHENTICATION_SUCCEEDED_CODE, probe.pollSignalCode())

                // 声明已收到第一条，因此只应回放第二条。
                probe.sendText(resumeRequestText(sequenceNumber = FIRST_SEQUENCE_NUMBER))

                assertEquals(
                    SECOND_SEQUENCE_NUMBER.toString(),
                    sequenceNumberTextOf(probe.pollMessage(RECEIVE_TIMEOUT_MILLISECONDS)),
                )
            } finally {
                probe.close()
            }
        }
    }

    @Test
    fun `an unpaired device is told that authentication failed`() {
        val loggedLines = CopyOnWriteArrayList<String>()
        withRunningEventServer(InMemoryRemoteEventStream(), logSink = loggedLines::add) { port ->
            val probe = JdkWebSocketProbe.connect(port)
            try {
                probe.sendText(connectRequestText())
                probe.sendText(authenticateRequestText(UNPAIRED_DEVICE_IDENTIFIER))

                assertEquals(AUTHENTICATION_FAILED_CODE, probe.pollSignalCode())
            } finally {
                probe.close()
            }
        }

        // 被拒这件事必须留下设备身份：此前这条通道在两端都不出声，连不上只能靠猜。
        assertTrue(loggedLines.any { line -> line.contains(UNPAIRED_DEVICE_IDENTIFIER) })
    }

    @Test
    fun `a client close frame releases the connection slot immediately`() {
        val connectionRegistry = RemoteConnectionRegistry()
        val eventStream = InMemoryRemoteEventStream()

        withRunningEventServer(eventStream, connectionRegistry = connectionRegistry) { port ->
            val probe = JdkWebSocketProbe.connect(port)
            try {
                probe.sendText(connectRequestText())
                probe.sendText(authenticateRequestText(PAIRED_DEVICE_IDENTIFIER.value))
                assertEquals(AUTHENTICATION_SUCCEEDED_CODE, probe.pollSignalCode())
                probe.sendText(resumeRequestText(sequenceNumber = NO_RECEIVED_EVENT_SEQUENCE_NUMBER))

                // 认证 + 续传完成后，连接名额已占、设备已关联。
                assertEquals(1, connectionRegistry.connectedDeviceCount())

                probe.close()

                // 客户端发了 Close 帧后，插件端必须立刻收尾——不能等 ping 超时（最多 30s）才释放名额。
                assertConnectionCountEventually(connectionRegistry, expected = 0)
            } finally {
                probe.close()
            }
        }
    }

    @Test
    fun `a resume request outside the retained window asks for a snapshot`() {
        val eventStream = InMemoryRemoteEventStream(maximumRetainedEventCount = RETAINED_EVENT_COUNT)
        eventStream.appendEvent(event(sequenceNumber = 1))
        eventStream.appendEvent(event(sequenceNumber = 2))
        eventStream.appendEvent(event(sequenceNumber = 3))

        withRunningEventServer(eventStream) { port ->
            val probe = JdkWebSocketProbe.connect(port)
            try {
                probe.sendText(connectRequestText())
                probe.sendText(authenticateRequestText(PAIRED_DEVICE_IDENTIFIER.value))
                assertEquals(AUTHENTICATION_SUCCEEDED_CODE, probe.pollSignalCode())

                // 保留窗口只覆盖 2 与 3，声明已收到 0 的客户端已经错过区间。
                probe.sendText(resumeRequestText(sequenceNumber = NO_RECEIVED_EVENT_SEQUENCE_NUMBER))

                assertEquals(EVENTS_NO_LONGER_AVAILABLE_CODE, probe.pollSignalCode())
                assertEquals(SNAPSHOT_REQUIRED_CODE, probe.pollSignalCode())
            } finally {
                probe.close()
            }
        }
    }

    private fun withRunningEventServer(
        eventStream: RemoteEventStream,
        logSink: (String) -> Unit = {},
        connectionRegistry: RemoteConnectionRegistry = RemoteConnectionRegistry(),
        verification: (Int) -> Unit,
    ) {
        val webSocketServer =
            RemoteWebSocketServer(
                controlGate = createControlGate(),
                connectionRegistry = connectionRegistry,
                eventStream = eventStream,
                logSink = logSink,
            )
        val server =
            embeddedServer(
                factory = CIO,
                port = 0,
                host = LOOPBACK_HOST_ADDRESS,
                module = { webSocketServer.installTo(this) },
            ).start(wait = false)
        try {
            // 端口 0 让内核分配空闲端口，测试因此不会与真实实例抢 9000。
            val boundPort = runBlocking { server.engine.resolvedConnectors().first().port }
            verification(boundPort)
        } finally {
            server.stop(STOP_GRACE_PERIOD_MILLISECONDS, STOP_TIMEOUT_MILLISECONDS)
        }
    }

    private fun createControlGate(): RemoteControlGate =
        RemoteControlGate(
            pairedDeviceRegistry =
                PairedDeviceRegistry().apply { recordPairedDevice(PAIRED_DEVICE_IDENTIFIER, TEST_INSTANT) },
            operationLog = InMemoryRemoteOperationLog(),
            rateLimiter = RemoteDeviceRateLimiter(Clock.fixed(TEST_INSTANT, ZoneOffset.UTC)),
            auditLogger = RemoteAuditLogger(auditSink = {}),
        )

    private fun event(sequenceNumber: Long): RemoteEventEnvelope =
        RemoteEventEnvelope(
            protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
            eventIdentifier = EventIdentifier("event_$sequenceNumber"),
            sequenceNumber = sequenceNumber,
            occurredAt = TEST_INSTANT,
            eventType = "history.item.observed",
            aggregateType = AggregateType.History,
            aggregateIdentifier = AggregateIdentifier("history_$sequenceNumber"),
        )

    private fun connectRequestText(): String =
        encodeClientMessage(
            RemoteClientMessage(protocolVersion = PROTOCOL_VERSION, messageType = RemoteMessageType.Connect),
        )

    private fun authenticateRequestText(deviceIdentifier: String): String =
        encodeClientMessage(
            RemoteClientMessage(
                protocolVersion = PROTOCOL_VERSION,
                messageType = RemoteMessageType.Authenticate,
                deviceIdentifier = DeviceIdentifier(deviceIdentifier),
            ),
        )

    private fun resumeRequestText(sequenceNumber: Long): String =
        encodeClientMessage(
            RemoteClientMessage(
                protocolVersion = PROTOCOL_VERSION,
                messageType = RemoteMessageType.Resume,
                sequenceNumber = sequenceNumber,
            ),
        )

    private fun encodeClientMessage(clientMessage: RemoteClientMessage): String =
        RemoteProtocolJson.instance.encodeToString(RemoteClientMessage.serializer(), clientMessage)

    private fun JdkWebSocketProbe.pollSignalCode(): String? =
        pollMessage(RECEIVE_TIMEOUT_MILLISECONDS)
            ?.let { message -> RemoteProtocolJson.instance.parseToJsonElement(message).jsonObject }
            ?.get("code")
            ?.jsonPrimitive
            ?.content

    private fun sequenceNumberTextOf(eventMessage: String?): String? =
        eventMessage
            ?.let { message -> RemoteProtocolJson.instance.parseToJsonElement(message).jsonObject }
            ?.get("sequenceNumber")
            ?.jsonPrimitive
            ?.content

    // 轮询而不是 sleep 固定时长：收尾通常毫秒级完成，轮询让测试快，只有真出问题才等到超时。
    private fun assertConnectionCountEventually(
        connectionRegistry: RemoteConnectionRegistry,
        expected: Int,
    ) {
        val deadline = System.currentTimeMillis() + CONNECTION_SETTLE_TIMEOUT_MILLISECONDS
        while (System.currentTimeMillis() < deadline) {
            if (connectionRegistry.connectedDeviceCount() == expected) return
            Thread.sleep(CONNECTION_POLL_INTERVAL_MILLISECONDS)
        }
        assertEquals(expected, connectionRegistry.connectedDeviceCount())
    }

    private companion object {
        private val TEST_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")

        private const val PROTOCOL_VERSION = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION

        private val PAIRED_DEVICE_IDENTIFIER = DeviceIdentifier("device_0000000000000001")

        private const val UNPAIRED_DEVICE_IDENTIFIER = "device_ffffffffffffffff"

        private const val LOOPBACK_HOST_ADDRESS = "127.0.0.1"

        private const val FIRST_SEQUENCE_NUMBER = 1L

        private const val SECOND_SEQUENCE_NUMBER = 2L

        private const val NO_RECEIVED_EVENT_SEQUENCE_NUMBER = 0L

        private const val RETAINED_EVENT_COUNT = 2

        private const val RECEIVE_TIMEOUT_MILLISECONDS = 2_000L

        // 连接收尾应该在毫秒级完成；给 3 秒余量排除 CI 抖动，但远小于 ping 超时的 30 秒。
        private const val CONNECTION_SETTLE_TIMEOUT_MILLISECONDS = 3_000L
        private const val CONNECTION_POLL_INTERVAL_MILLISECONDS = 20L

        private const val STOP_GRACE_PERIOD_MILLISECONDS = 500L

        private const val STOP_TIMEOUT_MILLISECONDS = 1_000L

        private const val AUTHENTICATION_SUCCEEDED_CODE = "authentication_succeeded"

        private const val AUTHENTICATION_FAILED_CODE = "authentication_failed"

        private const val EVENTS_NO_LONGER_AVAILABLE_CODE = "events_no_longer_available"

        private const val SNAPSHOT_REQUIRED_CODE = "snapshot_required"
    }
}

// JDK 内置的 WebSocket 客户端：仓库没有 Ktor 客户端 WebSocket 插件，而这里只需要收发文本帧。
private class JdkWebSocketProbe private constructor(
    private val webSocket: WebSocket,
    private val receivedMessages: LinkedBlockingQueue<String>,
) {
    fun sendText(text: String) {
        webSocket.sendText(text, true).join()
    }

    fun pollMessage(timeoutMilliseconds: Long): String? =
        receivedMessages.poll(
            timeoutMilliseconds,
            TimeUnit.MILLISECONDS,
        )

    fun close() {
        try {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, CLOSING_REASON).join()
        } catch (expectedClosedOutput: IOException) {
            // 服务端可能已先关闭（例如认证失败），此时关闭请求无处可发，测试不需要关心。
        } catch (expectedClosedOutput: CompletionException) {
            // join 把 sendClose 的失败裹成 CompletionException；只有底层是 IO 故障才是「对端已关」，其余照抛。
            if (expectedClosedOutput.cause !is IOException) {
                throw expectedClosedOutput
            }
        }
    }

    // 分片文本按 last 标志合并，否则被拆帧的报文会被当成两条。
    private class MessageCollector : WebSocket.Listener {
        val receivedMessages = LinkedBlockingQueue<String>()

        private val partialText = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(
            webSocket: WebSocket,
            data: CharSequence,
            last: Boolean,
        ): CompletionStage<*>? {
            partialText.append(data)
            if (last) {
                receivedMessages.add(partialText.toString())
                partialText.setLength(0)
            }
            webSocket.request(1)
            return null
        }
    }

    companion object {
        private const val CLOSING_REASON = "测试结束"

        fun connect(port: Int): JdkWebSocketProbe {
            val messageCollector = MessageCollector()
            val webSocket =
                httpClient
                    .newWebSocketBuilder()
                    .buildAsync(URI.create("ws://127.0.0.1:$port/v1/events"), messageCollector)
                    .join()
            return JdkWebSocketProbe(webSocket, messageCollector.receivedMessages)
        }

        // 复用同一个客户端：每个连接新建一个会留下大量选择器线程。
        private val httpClient: HttpClient = HttpClient.newHttpClient()
    }
}
