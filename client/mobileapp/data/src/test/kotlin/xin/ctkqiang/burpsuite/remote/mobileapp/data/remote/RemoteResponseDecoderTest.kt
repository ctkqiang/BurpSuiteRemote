package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.CommandResult
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteError
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteErrorCode
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemotePayload
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteRuntimeState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ServerCapabilities

class RemoteResponseDecoderTest {
    @Test
    fun `a runtime state payload is decoded into the domain shape`() {
        val payload =
            jsonPayload(
                """{"protocolVersion":1,"remotePort":9000,"connectedDeviceCount":1,""" +
                    """"pairedDeviceCount":2,"latestEventSequenceNumber":7}""",
            )

        val result = RemoteResponseDecoder.decodeRuntimeState(payload)

        assertEquals(
            RemoteResult.Succeeded(
                RemoteRuntimeState(
                    protocolVersion = 1,
                    remotePort = 9000,
                    connectedDeviceCount = 1,
                    pairedDeviceCount = 2,
                    latestEventSequenceNumber = 7,
                ),
            ),
            result,
        )
    }

    @Test
    fun `a capabilities payload keeps every reported name`() {
        val payload = jsonPayload("""{"capabilities":["status","pairing","event-stream","snapshot","history"]}""")

        val result = RemoteResponseDecoder.decodeCapabilities(payload)

        assertEquals(
            RemoteResult.Succeeded(
                ServerCapabilities(
                    names = setOf("status", "pairing", "event-stream", "snapshot", "history"),
                ),
            ),
            result,
        )
    }

    @Test
    fun `a pairing payload becomes the issued device identity`() {
        val payload = jsonPayload("""{"deviceIdentifier":"device_abc"}""")

        val result = RemoteResponseDecoder.decodePairingIdentity(payload)

        assertEquals(RemoteResult.Succeeded(DeviceIdentifier("device_abc")), result)
    }

    @Test
    fun `a history payload is carried as its original json text`() {
        val payload = jsonPayload("""{"historyIdentifier":"history_1"}""")

        val result = RemoteResponseDecoder.decodeHistoryPayload(payload)

        assertEquals(RemoteResult.Succeeded(RemotePayload("""{"historyIdentifier":"history_1"}""")), result)
    }

    @Test
    fun `a payload that does not match the contract is malformed`() {
        val payload = jsonPayload("\"not an object\"")

        assertEquals(
            RemoteResult.Failed(RemoteFailure.MalformedServerResponse),
            RemoteResponseDecoder.decodeRuntimeState(payload),
        )
        assertEquals(
            RemoteResult.Failed(RemoteFailure.MalformedServerResponse),
            RemoteResponseDecoder.decodeRuntimeState(null),
        )
        assertEquals(
            RemoteResult.Failed(RemoteFailure.MalformedServerResponse),
            RemoteResponseDecoder.decodePairingIdentity(jsonPayload("""{"deviceIdentifier":12}""")),
        )
    }

    @Test
    fun `an envelope without a result carries no failure`() {
        assertNull(RemoteResponseDecoder.decodeFailure(envelopeWith(result = null)))
    }

    @Test
    fun `a rejected envelope keeps the rejection reason`() {
        val failure =
            RemoteResponseDecoder.decodeFailure(
                envelopeWith(result = CommandResult.Rejected(RejectionReason.DeviceNotPaired)),
            )

        assertEquals(RemoteFailure.DeviceNotPaired, failure)
    }

    @Test
    fun `a failed envelope is mapped through the error code`() {
        val failure =
            RemoteResponseDecoder.decodeFailure(
                envelopeWith(
                    result =
                        CommandResult.Failed(
                            RemoteError(RemoteErrorCode.NotImplemented, isRetryable = false),
                        ),
                ),
            )

        assertEquals(RemoteFailure.ActionNotSupported, failure)
    }

    @Test
    fun `a query response carrying a command success is malformed`() {
        val failure =
            RemoteResponseDecoder.decodeFailure(
                envelopeWith(result = CommandResult.Succeeded(OperationIdentifier("operation_1"))),
            )

        assertEquals(RemoteFailure.MalformedServerResponse, failure)
    }

    private fun jsonPayload(encodedJson: String): JsonElement = json.parseToJsonElement(encodedJson)

    private fun envelopeWith(result: CommandResult?): RemoteResponseEnvelope =
        RemoteResponseEnvelope(
            protocolVersion = 1,
            messageType = "query",
            result = result,
        )

    private companion object {
        val json = Json
    }
}
