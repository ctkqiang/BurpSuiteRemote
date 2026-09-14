package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.AddHistoryToScope
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.DropIntercept
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ExecuteRepeater
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ForwardIntercept
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.ShareHistory
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.PairingAttempt
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteRuntimeState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteTimeouts
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ServerCapabilities

/** 用真实时钟跑：MockEngine 在真实调度器上执行请求，runTest 的虚拟时钟会在等待期间直接跳到超时。 */
class RemoteRestClientTest {
    private val recordedRequests = mutableListOf<HttpRequestData>()

    @Test
    fun `a status response becomes the domain runtime state`() =
        runBlocking {
            val restClient = restClientRespondingWith(RUNTIME_STATE_RESPONSE)

            val result = restClient.readRuntimeState(CONFIGURATION)

            assertEquals(RemoteResult.Succeeded(RUNTIME_STATE), result)
            val request = recordedRequests.single()
            assertEquals(HttpMethod.Get, request.method)
            assertEquals(RemoteEndpointPath.STATUS, request.url.encodedPath)
            assertEquals(PAIRED_DEVICE.value, request.headers[DEVICE_IDENTIFIER_HEADER])
            // 查询端点不带操作标识：带了就等于声明一次命令执行，插件会记一笔审计。
            assertNull(request.headers[OPERATION_IDENTIFIER_HEADER])
        }

    @Test
    fun `a capabilities response becomes the reported capability names`() =
        runBlocking {
            val restClient = restClientRespondingWith(CAPABILITIES_RESPONSE)

            val result = restClient.readCapabilities(CONFIGURATION)

            assertEquals(RemoteResult.Succeeded(ServerCapabilities(setOf("status", "history"))), result)
            assertEquals(RemoteEndpointPath.CAPABILITIES, recordedRequests.single().url.encodedPath)
        }

    @Test
    fun `a snapshot response is decoded through the same shape as the status`() =
        runBlocking {
            val restClient = restClientRespondingWith(RUNTIME_STATE_RESPONSE)

            val result = restClient.requestSnapshot(CONFIGURATION)

            assertEquals(RemoteResult.Succeeded(RUNTIME_STATE), result)
            assertEquals(RemoteEndpointPath.SNAPSHOT, recordedRequests.single().url.encodedPath)
        }

    @Test
    fun `a pairing response becomes the issued device identity`() =
        runBlocking {
            val restClient = restClientRespondingWith(PAIRING_RESPONSE, deviceIdentifier = null)

            val result = restClient.pair(PAIRING_ATTEMPT)

            assertEquals(RemoteResult.Succeeded(DeviceIdentifier("device_abc")), result)
            val request = recordedRequests.single()
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(RemoteEndpointPath.PAIR, request.url.encodedPath)
            // 配对请求还没有身份可用，这一条不能白白带个空头过去。
            assertNull(request.headers[DEVICE_IDENTIFIER_HEADER])
        }

    @Test
    fun `an unpaired device is reported as not paired rather than as a transport error`() =
        runBlocking {
            val restClient = restClientRespondingWith(REJECTED_DEVICE_NOT_PAIRED_RESPONSE)

            val result = restClient.readRuntimeState(CONFIGURATION)

            assertEquals(RemoteResult.Failed(RemoteFailure.DeviceNotPaired), result)
        }

    @Test
    fun `a dispatched command carries its own operation identifier to the plugin`() =
        runBlocking {
            val restClient = restClientRespondingWith(SUCCEEDED_COMMAND_RESPONSE)

            val result =
                restClient.dispatch(
                    CONFIGURATION,
                    ForwardIntercept(
                        interceptIdentifier = INTERCEPT_IDENTIFIER,
                        operationIdentifier = FIXED_OPERATION_IDENTIFIER,
                    ),
                )

            assertEquals(RemoteResult.Succeeded(Unit), result)
            val request = recordedRequests.single()
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("${RemoteEndpointPath.INTERCEPTS}/intercept_1/forward", request.url.encodedPath)
            assertEquals(PAIRED_DEVICE.value, request.headers[DEVICE_IDENTIFIER_HEADER])
            assertEquals(FIXED_OPERATION_IDENTIFIER.value, request.headers[OPERATION_IDENTIFIER_HEADER])
        }

    @Test
    fun `a scope command addresses the history item whose host is to be included`() =
        runBlocking {
            val restClient = restClientRespondingWith(SUCCEEDED_COMMAND_RESPONSE)

            val result =
                restClient.dispatch(
                    CONFIGURATION,
                    AddHistoryToScope(
                        historyIdentifier = HISTORY_IDENTIFIER,
                        operationIdentifier = FIXED_OPERATION_IDENTIFIER,
                    ),
                )

            assertEquals(RemoteResult.Succeeded(Unit), result)
            assertEquals("${RemoteEndpointPath.SCOPE}/history_1", recordedRequests.single().url.encodedPath)
        }

    @Test
    fun `a repeater execution command addresses the request by its identifier`() =
        runBlocking {
            val restClient = restClientRespondingWith(SUCCEEDED_COMMAND_RESPONSE)

            val result =
                restClient.dispatch(
                    CONFIGURATION,
                    ExecuteRepeater(
                        repeaterRequestIdentifier = RepeaterRequestIdentifier("repeater_7"),
                        operationIdentifier = FIXED_OPERATION_IDENTIFIER,
                    ),
                )

            assertEquals(RemoteResult.Succeeded(Unit), result)
            assertEquals("${RemoteEndpointPath.REPEATER}/repeater_7/execute", recordedRequests.single().url.encodedPath)
        }

    @Test
    fun `a rejection of a command without an operation identifier is reported to the caller`() =
        runBlocking {
            val restClient = restClientRespondingWith(REJECTED_MISSING_OPERATION_IDENTIFIER_RESPONSE)

            val result =
                restClient.dispatch(
                    CONFIGURATION,
                    DropIntercept(
                        interceptIdentifier = INTERCEPT_IDENTIFIER,
                        operationIdentifier = FIXED_OPERATION_IDENTIFIER,
                    ),
                )

            assertEquals(RemoteResult.Failed(RemoteFailure.MissingOperationIdentifier), result)
        }

    @Test
    fun `an unimplemented command is reported as an unsupported action`() =
        runBlocking {
            val restClient = restClientRespondingWith(FAILED_NOT_IMPLEMENTED_RESPONSE)

            val result =
                restClient.dispatch(
                    CONFIGURATION,
                    ExecuteRepeater(
                        repeaterRequestIdentifier = RepeaterRequestIdentifier("repeater_7"),
                        operationIdentifier = FIXED_OPERATION_IDENTIFIER,
                    ),
                )

            assertEquals(RemoteResult.Failed(RemoteFailure.ActionNotSupported), result)
        }

    @Test
    fun `a command the plugin has no route for never leaves the phone`() =
        runBlocking {
            val restClient = restClientRespondingWith(SUCCEEDED_COMMAND_RESPONSE)

            val result =
                restClient.dispatch(
                    CONFIGURATION,
                    ShareHistory(
                        historyIdentifier = HISTORY_IDENTIFIER,
                        operationIdentifier = FIXED_OPERATION_IDENTIFIER,
                    ),
                )

            assertEquals(RemoteResult.Failed(RemoteFailure.ActionNotSupported), result)
            assertTrue(recordedRequests.isEmpty())
        }

    @Test
    fun `a server error status is reported as an internal failure`() =
        runBlocking {
            val restClient = restClientRespondingWith("", responseStatus = HttpStatusCode.InternalServerError)

            val result = restClient.readRuntimeState(CONFIGURATION)

            assertEquals(RemoteResult.Failed(RemoteFailure.ServerInternalFailure), result)
        }

    @Test
    fun `an unsupported protocol version is refused before the payload is read`() =
        runBlocking {
            val restClient =
                restClientRespondingWith(
                    RUNTIME_STATE_RESPONSE.replace(VERSION_FIELD, UNSUPPORTED_VERSION),
                )

            val result = restClient.readRuntimeState(CONFIGURATION)

            assertEquals(RemoteResult.Failed(RemoteFailure.ProtocolVersionUnsupported), result)
        }

    @Test
    fun `a response that is not an envelope is reported as malformed`() =
        runBlocking {
            val restClient = restClientRespondingWith("not json")

            val result = restClient.readRuntimeState(CONFIGURATION)

            assertEquals(RemoteResult.Failed(RemoteFailure.MalformedServerResponse), result)
        }

    private fun restClientRespondingWith(
        responseBody: String,
        responseStatus: HttpStatusCode = HttpStatusCode.OK,
        deviceIdentifier: DeviceIdentifier? = PAIRED_DEVICE,
    ): RemoteRestClient =
        RemoteRestClient(
            httpClient =
                RemoteHttpClientFactory.create(
                    MockEngine { request ->
                        recordedRequests += request
                        respond(
                            content = responseBody,
                            status = responseStatus,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                ),
            deviceIdentifierProvider = RemoteDeviceIdentifierProvider { deviceIdentifier },
            timeouts = RemoteTimeouts(),
            operationIdentifierGenerator = OperationIdentifierGenerator { FIXED_OPERATION_IDENTIFIER },
        )

    private companion object {
        const val DEVICE_IDENTIFIER_HEADER = "X-Burp-Remote-Device-Identifier"

        const val OPERATION_IDENTIFIER_HEADER = "X-Burp-Remote-Operation-Identifier"

        const val VERSION_FIELD = "\"protocolVersion\":1"

        const val UNSUPPORTED_VERSION = "\"protocolVersion\":99"

        val PAIRED_DEVICE = DeviceIdentifier("device_0000000000000001")

        val FIXED_OPERATION_IDENTIFIER = OperationIdentifier("operation_0000000000000001")

        val INTERCEPT_IDENTIFIER = InterceptIdentifier("intercept_1")

        val HISTORY_IDENTIFIER = HistoryIdentifier("history_1")

        val CONFIGURATION =
            RemoteConnectionConfiguration(host = "127.0.0.1", port = 9000, deviceName = "test-device")

        val PAIRING_ATTEMPT =
            PairingAttempt(
                host = CONFIGURATION.host,
                port = CONFIGURATION.port,
                challengeIdentifier = PairingChallengeIdentifier("challenge_1"),
                pairingCode = PairingCode("pairing_code_1"),
            )

        val RUNTIME_STATE =
            RemoteRuntimeState(
                protocolVersion = 1,
                remotePort = 9000,
                connectedDeviceCount = 1,
                pairedDeviceCount = 2,
                latestEventSequenceNumber = 7,
            )

        // 应答体照抄插件 RemoteResponse 的线上写法：encodeDefaults 打开，因此空字段也会出现。
        const val RUNTIME_STATE_RESPONSE =
            """{"protocolVersion":1,"messageType":"query","result":null,""" +
                """"payload":{"protocolVersion":1,"remotePort":9000,"connectedDeviceCount":1,""" +
                """"pairedDeviceCount":2,"latestEventSequenceNumber":7}}"""

        const val CAPABILITIES_RESPONSE =
            """{"protocolVersion":1,"messageType":"query","result":null,""" +
                """"payload":{"capabilities":["status","history"]}}"""

        const val PAIRING_RESPONSE =
            """{"protocolVersion":1,"messageType":"pair","result":null,""" +
                """"payload":{"deviceIdentifier":"device_abc"}}"""

        const val REJECTED_DEVICE_NOT_PAIRED_RESPONSE =
            """{"protocolVersion":1,"messageType":"query",""" +
                """"result":{"type":"rejected","reason":"device_not_paired"},"payload":null}"""

        const val REJECTED_MISSING_OPERATION_IDENTIFIER_RESPONSE =
            """{"protocolVersion":1,"messageType":"command",""" +
                """"result":{"type":"rejected","reason":"missing_operation_identifier"},"payload":null}"""

        const val SUCCEEDED_COMMAND_RESPONSE =
            """{"protocolVersion":1,"messageType":"command",""" +
                """"result":{"type":"succeeded","operationIdentifier":"operation_0000000000000001"},"payload":null}"""

        const val FAILED_NOT_IMPLEMENTED_RESPONSE =
            """{"protocolVersion":1,"messageType":"command",""" +
                """"result":{"type":"failed","error":{"code":"not_implemented","isRetryable":false}},"payload":null}"""
    }
}
