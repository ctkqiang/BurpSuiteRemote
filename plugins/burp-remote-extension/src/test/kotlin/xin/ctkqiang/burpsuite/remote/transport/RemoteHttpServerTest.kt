/**
 * REST 路由测试：读取类端点鉴权、未实现端点诚实失败、控制命令缺操作标识被拒、配对成功回到设备身份。
 * 用 Ktor 测试宿主，不占真实端口（rules.md §13）。
 */

package xin.ctkqiang.burpsuite.remote.transport

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.adapter.BurpHistoryAdapter
import xin.ctkqiang.burpsuite.remote.adapter.BurpProxyHistorySource
import xin.ctkqiang.burpsuite.remote.adapter.BurpScopeAdapter
import xin.ctkqiang.burpsuite.remote.adapter.BurpScopeWriter
import xin.ctkqiang.burpsuite.remote.adapter.StubProxyHistoryEntry
import xin.ctkqiang.burpsuite.remote.adapter.StubProxyHistorySource
import xin.ctkqiang.burpsuite.remote.adapter.StubScopeWriter
import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolJson
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.security.DevicePairingService
import xin.ctkqiang.burpsuite.remote.security.LocalNetworkAddressResolver
import xin.ctkqiang.burpsuite.remote.security.PairedDeviceRegistry
import java.net.ServerSocket
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RemoteHttpServerTest {
    @Test
    fun `a query without a paired device is rejected`() =
        testApplication {
            application { createServer().installTo(this) }

            val response = client.get(STATUS_PATH)

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(DEVICE_NOT_PAIRED_CODE, rejectionReasonOf(response.bodyAsText()))
        }

    @Test
    fun `history returns the burp entries for a paired device`() =
        testApplication {
            application { createServer().installTo(this) }

            val response =
                client.get(HISTORY_PATH) {
                    header(DEVICE_IDENTIFIER_HEADER, PAIRED_DEVICE.value)
                }

            val historyItems = payloadOf(response.bodyAsText())?.get(HISTORY_ITEMS_FIELD)?.jsonArray
            assertEquals(1, historyItems?.size)
            assertEquals(StubProxyHistoryEntry.DEFAULT_PATH, historyItems?.get(0).textAt(PATH_FIELD))
        }

    @Test
    fun `history is not readable without a paired device`() =
        testApplication {
            application { createServer().installTo(this) }

            val response = client.get(HISTORY_PATH)

            assertEquals(DEVICE_NOT_PAIRED_CODE, rejectionReasonOf(response.bodyAsText()))
        }

    @Test
    fun `a history message is fetched in full by its identifier`() =
        testApplication {
            val historySource = StubProxyHistorySource()
            historySource.appendEntry(StubProxyHistoryEntry())
            application { createServer(historySource = historySource).installTo(this) }
            val historyIdentifier = BurpHistoryAdapter(historySource).toHistoryIdentifier(StubProxyHistoryEntry())

            val response =
                client.get("$HISTORY_PATH/${historyIdentifier.value}") {
                    header(DEVICE_IDENTIFIER_HEADER, PAIRED_DEVICE.value)
                }

            assertEquals(
                StubProxyHistoryEntry.DEFAULT_RESPONSE_BODY_TEXT,
                payloadOf(response.bodyAsText()).textAt(RESPONSE_BODY_FIELD),
            )
        }

    @Test
    fun `an unknown history identifier fails without inventing a record`() =
        testApplication {
            application { createServer().installTo(this) }

            val response =
                client.get("$HISTORY_PATH/$UNKNOWN_HISTORY_IDENTIFIER") {
                    header(DEVICE_IDENTIFIER_HEADER, PAIRED_DEVICE.value)
                }

            assertEquals(BURP_RUNTIME_FAILURE_CODE, failureCodeOf(response.bodyAsText()))
        }

    @Test
    fun `including a known history entry in the scope writes exactly its host to the burp scope`() =
        testApplication {
            val historySource = StubProxyHistorySource(mutableListOf(StubProxyHistoryEntry()))
            val scopeWriter = StubScopeWriter()
            application {
                createServer(historySource = historySource, scopeWriter = scopeWriter).installTo(this)
            }
            val historyIdentifier = BurpHistoryAdapter(historySource).toHistoryIdentifier(StubProxyHistoryEntry())

            val response =
                client.post("$SCOPE_PATH/${historyIdentifier.value}") {
                    header(DEVICE_IDENTIFIER_HEADER, PAIRED_DEVICE.value)
                    header(OPERATION_IDENTIFIER_HEADER, OPERATION_IDENTIFIER_TEXT)
                }

            assertEquals(OPERATION_IDENTIFIER_TEXT, operationIdentifierOf(response.bodyAsText()))
            assertEquals(listOf("https://${StubProxyHistoryEntry.DEFAULT_HOST}"), scopeWriter.includedHostTexts)
        }

    @Test
    fun `an unknown history identifier cannot be added to the scope`() =
        testApplication {
            val scopeWriter = StubScopeWriter()
            application { createServer(scopeWriter = scopeWriter).installTo(this) }

            val response =
                client.post("$SCOPE_PATH/$UNKNOWN_HISTORY_IDENTIFIER") {
                    header(DEVICE_IDENTIFIER_HEADER, PAIRED_DEVICE.value)
                    header(OPERATION_IDENTIFIER_HEADER, OPERATION_IDENTIFIER_TEXT)
                }

            assertEquals(BURP_RUNTIME_FAILURE_CODE, failureCodeOf(response.bodyAsText()))
            assertTrue(scopeWriter.includedHostTexts.isEmpty())
        }

    @Test
    fun `replaying the same scope command answers with the first result and writes the host only once`() =
        testApplication {
            val historySource = StubProxyHistorySource(mutableListOf(StubProxyHistoryEntry()))
            val scopeWriter = StubScopeWriter()
            application {
                createServer(historySource = historySource, scopeWriter = scopeWriter).installTo(this)
            }
            val historyIdentifier = BurpHistoryAdapter(historySource).toHistoryIdentifier(StubProxyHistoryEntry())
            val scopeIncludePath = "$SCOPE_PATH/${historyIdentifier.value}"

            repeat(2) {
                val replayedResponse =
                    client.post(scopeIncludePath) {
                        header(DEVICE_IDENTIFIER_HEADER, PAIRED_DEVICE.value)
                        header(OPERATION_IDENTIFIER_HEADER, OPERATION_IDENTIFIER_TEXT)
                    }

                assertEquals(OPERATION_IDENTIFIER_TEXT, operationIdentifierOf(replayedResponse.bodyAsText()))
            }

            assertEquals(1, scopeWriter.includedHostTexts.size)
        }

    @Test
    fun `an unpaired device cannot add a history entry to the scope`() =
        testApplication {
            val scopeWriter = StubScopeWriter()
            application { createServer(scopeWriter = scopeWriter).installTo(this) }

            val response =
                client.post("$SCOPE_PATH/$UNKNOWN_HISTORY_IDENTIFIER") {
                    header(OPERATION_IDENTIFIER_HEADER, OPERATION_IDENTIFIER_TEXT)
                }

            assertEquals(DEVICE_NOT_PAIRED_CODE, rejectionReasonOf(response.bodyAsText()))
            assertTrue(scopeWriter.includedHostTexts.isEmpty())
        }

    @Test
    fun `intercepts honestly report that the burp adapter is not implemented`() =
        testApplication {
            application { createServer().installTo(this) }

            val response =
                client.get(INTERCEPTS_PATH) {
                    header(DEVICE_IDENTIFIER_HEADER, PAIRED_DEVICE.value)
                }

            assertEquals(NOT_IMPLEMENTED_CODE, failureCodeOf(response.bodyAsText()))
        }

    @Test
    fun `an unpaired device cannot submit a control command`() =
        testApplication {
            application { createServer().installTo(this) }

            val response =
                client.post(INTERCEPT_FORWARD_PATH) {
                    header(OPERATION_IDENTIFIER_HEADER, OPERATION_IDENTIFIER_TEXT)
                }

            assertEquals(DEVICE_NOT_PAIRED_CODE, rejectionReasonOf(response.bodyAsText()))
        }

    @Test
    fun `a control command without an operation identifier is rejected`() =
        testApplication {
            application { createServer().installTo(this) }

            val response =
                client.post(INTERCEPT_FORWARD_PATH) {
                    header(DEVICE_IDENTIFIER_HEADER, PAIRED_DEVICE.value)
                }

            assertEquals(MISSING_OPERATION_IDENTIFIER_CODE, rejectionReasonOf(response.bodyAsText()))
        }

    @Test
    fun `a valid pairing code yields the new device identity`() =
        testApplication {
            val devicePairingService = createPairingService()
            val pairingTicket = devicePairingService.openPairingSession()
            application { createServer(devicePairingService).installTo(this) }

            val response =
                client.post(PAIR_PATH) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        pairingRequestText(
                            challengeIdentifier = pairingTicket.challengeIdentifier.value,
                            pairingCode = pairingTicket.pairingCode.value,
                        ),
                    )
                }

            val payload = payloadOf(response.bodyAsText())
            assertTrue(payload?.get(DEVICE_IDENTIFIER_FIELD)?.jsonPrimitive?.content?.startsWith("device_") == true)
        }

    @Test
    fun `starting on an occupied port fails without throwing and says so in the log`() {
        val occupiedSocket = ServerSocket(0)
        try {
            val logMessages = mutableListOf<String>()
            val server = createServer(remotePort = occupiedSocket.localPort, logSink = logMessages::add)

            assertFalse(server.start())
            assertFalse(server.isRunning)
            assertTrue(logMessages.any { message -> message.contains(OCCUPIED_PORT_LOG_TEXT) })
        } finally {
            occupiedSocket.close()
        }
    }

    @Test
    fun `stopping releases the port so the endpoint can be started again`() {
        val remotePort = ServerSocket(0).use { socket -> socket.localPort }
        val server = createServer(remotePort = remotePort)
        assertTrue(server.start())
        assertTrue(server.isRunning)

        server.stop()
        assertFalse(server.isRunning)

        // 同一个端口能再次绑定，才说明 stop 真的释放了端口而不是留着一个占位的监听线程。
        val restartedServer = createServer(remotePort = remotePort)
        try {
            assertTrue(restartedServer.start())
        } finally {
            restartedServer.stop()
        }
    }

    private fun createServer(
        devicePairingService: DevicePairingService = createPairingService(),
        historySource: BurpProxyHistorySource = StubProxyHistorySource(mutableListOf(StubProxyHistoryEntry())),
        scopeWriter: BurpScopeWriter = StubScopeWriter(),
        remotePort: Int = DEFAULT_REMOTE_PORT,
        logSink: (String) -> Unit = {},
    ): RemoteHttpServer {
        val pairedDeviceRegistry =
            PairedDeviceRegistry().apply { recordPairedDevice(PAIRED_DEVICE, TEST_INSTANT) }
        val historyAdapter = BurpHistoryAdapter(historySource)
        return RemoteHttpServer(
            devicePairingService = devicePairingService,
            pairedDeviceRegistry = pairedDeviceRegistry,
            controlGate =
                RemoteControlGate(
                    pairedDeviceRegistry = pairedDeviceRegistry,
                    operationLog = InMemoryRemoteOperationLog(),
                    rateLimiter = RemoteDeviceRateLimiter(TEST_CLOCK),
                    auditLogger = RemoteAuditLogger(auditSink = {}),
                ),
            connectionRegistry = RemoteConnectionRegistry(),
            eventStream = InMemoryRemoteEventStream(),
            historyAdapter = historyAdapter,
            scopeAdapter = BurpScopeAdapter(historyAdapter, scopeWriter),
            logSink = logSink,
            remotePort = remotePort,
        )
    }

    private fun createPairingService(): DevicePairingService =
        DevicePairingService(
            localNetworkAddressResolver = FixedLocalNetworkAddressResolver(),
            pairedDeviceRegistry = PairedDeviceRegistry(),
            clock = TEST_CLOCK,
        )

    private fun pairingRequestText(
        challengeIdentifier: String,
        pairingCode: String,
    ): String =
        """
        {
          "protocolVersion": ${RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION},
          "messageType": "pair",
          "challengeIdentifier": "$challengeIdentifier",
          "pairingCode": "$pairingCode"
        }
        """.trimIndent()

    private fun resultOf(responseBody: String): JsonObject? =
        RemoteProtocolJson.instance.parseToJsonElement(responseBody).jsonObject["result"]?.jsonObject

    private fun rejectionReasonOf(responseBody: String): String? =
        resultOf(responseBody)?.get("reason")?.jsonPrimitive?.content

    private fun failureCodeOf(responseBody: String): String? =
        resultOf(responseBody)?.get("error")?.jsonObject?.get("code")?.jsonPrimitive?.content

    private fun operationIdentifierOf(responseBody: String): String? =
        resultOf(responseBody)?.get(OPERATION_IDENTIFIER_FIELD)?.jsonPrimitive?.content

    private fun payloadOf(responseBody: String): JsonObject? =
        RemoteProtocolJson.instance.parseToJsonElement(responseBody).jsonObject["payload"]?.jsonObject

    private fun JsonElement?.textAt(fieldName: String): String? =
        this?.jsonObject?.get(
            fieldName,
        )?.jsonPrimitive?.content

    private companion object {
        private val TEST_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")

        private val TEST_CLOCK: Clock = Clock.fixed(TEST_INSTANT, ZoneOffset.UTC)

        private val PAIRED_DEVICE = DeviceIdentifier("device_0000000000000001")

        private const val OPERATION_IDENTIFIER_TEXT = "operation_0000000000000001"

        private const val STATUS_PATH = "/v1/status"

        private const val PAIR_PATH = "/v1/pair"

        private const val HISTORY_PATH = "/v1/history"

        private const val SCOPE_PATH = "/v1/scope"

        private const val INTERCEPTS_PATH = "/v1/intercepts"

        private const val HISTORY_ITEMS_FIELD = "historyItems"

        private const val PATH_FIELD = "path"

        private const val RESPONSE_BODY_FIELD = "responseBody"

        private const val UNKNOWN_HISTORY_IDENTIFIER = "history_ffffffffffffffff"

        private const val INTERCEPT_FORWARD_PATH = "/v1/intercepts/intercept_1/forward"

        private const val DEVICE_IDENTIFIER_HEADER = "X-Burp-Remote-Device-Identifier"

        private const val OPERATION_IDENTIFIER_HEADER = "X-Burp-Remote-Operation-Identifier"

        private const val DEVICE_IDENTIFIER_FIELD = "deviceIdentifier"

        private const val OPERATION_IDENTIFIER_FIELD = "operationIdentifier"

        private const val DEVICE_NOT_PAIRED_CODE = "device_not_paired"

        private const val MISSING_OPERATION_IDENTIFIER_CODE = "missing_operation_identifier"

        private const val NOT_IMPLEMENTED_CODE = "not_implemented"

        private const val BURP_RUNTIME_FAILURE_CODE = "burp_runtime_failure"

        private const val OCCUPIED_PORT_LOG_TEXT = "启动失败"
    }
}

// 地址写死，用例就不随跑测机器的网卡状态变化。
private class FixedLocalNetworkAddressResolver : LocalNetworkAddressResolver {
    override fun resolveAdvertisedHostAddress(): String = "192.168.1.12"
}
