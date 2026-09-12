package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteError
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteErrorCode
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure

class RemoteFailureMapperTest {
    @Test
    fun `every rejection reason maps to a distinct domain failure`() {
        val expectedFailures =
            mapOf(
                RejectionReason.DeviceNotPaired to RemoteFailure.DeviceNotPaired,
                RejectionReason.DeviceNotAuthorized to RemoteFailure.DeviceNotAuthorized,
                RejectionReason.RateLimitExceeded to RemoteFailure.RateLimited,
                RejectionReason.TargetNotAvailable to RemoteFailure.TargetUnavailable,
                RejectionReason.UnsupportedProtocolVersion to RemoteFailure.ProtocolVersionUnsupported,
                RejectionReason.PairingSessionNotAvailable to RemoteFailure.PairingSessionUnavailable,
                RejectionReason.PairingSessionExpired to RemoteFailure.PairingSessionExpired,
                RejectionReason.PairingCodeMismatch to RemoteFailure.PairingCodeRejected,
                RejectionReason.MissingOperationIdentifier to RemoteFailure.MissingOperationIdentifier,
            )

        // 键集相等保证映射是穷尽的：插件新增原因时这里会先红，而不是运行时掉进别的分支。
        assertEquals(RejectionReason.entries.toSet(), expectedFailures.keys)
        expectedFailures.forEach { (reason, expectedFailure) ->
            assertEquals(expectedFailure, RemoteFailureMapper.fromRejected(reason))
        }
    }

    @Test
    fun `not implemented is reported as an unsupported action rather than a network error`() {
        val failure = RemoteFailureMapper.fromFailed(RemoteError(RemoteErrorCode.NotImplemented, isRetryable = false))

        assertEquals(RemoteFailure.ActionNotSupported, failure)
    }

    @Test
    fun `every error code maps to a domain failure`() {
        val expectedFailures =
            mapOf(
                RemoteErrorCode.InternalFailure to RemoteFailure.ServerInternalFailure,
                RemoteErrorCode.BurpRuntimeFailure to RemoteFailure.BurpRuntimeFailure,
                RemoteErrorCode.SerializationFailure to RemoteFailure.ServerSerializationFailure,
                RemoteErrorCode.Timeout to RemoteFailure.TimedOut,
                RemoteErrorCode.NotImplemented to RemoteFailure.ActionNotSupported,
                RemoteErrorCode.RequestPayloadTooLarge to RemoteFailure.PayloadTooLarge,
            )

        assertEquals(RemoteErrorCode.entries.toSet(), expectedFailures.keys)
        expectedFailures.forEach { (code, expectedFailure) ->
            assertEquals(expectedFailure, RemoteFailureMapper.fromFailed(RemoteError(code, isRetryable = false)))
        }
    }

    @Test
    fun `http status codes map to the closest transport-level failure`() {
        assertEquals(RemoteFailure.PayloadTooLarge, RemoteFailureMapper.fromHttpStatusCode(413))
        assertEquals(RemoteFailure.ServerInternalFailure, RemoteFailureMapper.fromHttpStatusCode(500))
        assertEquals(RemoteFailure.MalformedServerResponse, RemoteFailureMapper.fromHttpStatusCode(404))
    }
}
