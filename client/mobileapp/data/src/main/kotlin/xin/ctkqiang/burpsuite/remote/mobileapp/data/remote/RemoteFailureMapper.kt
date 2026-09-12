package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteError
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteErrorCode
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure

/**
 * 插件机器码 → 领域失败。
 *
 * 「尚未实现」必须映射成一个可区分的失败：界面要能说「服务端还没做」，而不是把它混进网络错误里。
 * 映射是穷尽的：插件新增错误码时这里编译不过，比运行时悄悄掉进 else 强。
 */
object RemoteFailureMapper {
    /** 映射插件明确拒绝的原因。 */
    fun fromRejected(reason: RejectionReason): RemoteFailure =
        when (reason) {
            RejectionReason.DeviceNotPaired -> RemoteFailure.DeviceNotPaired
            RejectionReason.DeviceNotAuthorized -> RemoteFailure.DeviceNotAuthorized
            RejectionReason.RateLimitExceeded -> RemoteFailure.RateLimited
            RejectionReason.TargetNotAvailable -> RemoteFailure.TargetUnavailable
            RejectionReason.UnsupportedProtocolVersion -> RemoteFailure.ProtocolVersionUnsupported
            RejectionReason.PairingSessionNotAvailable -> RemoteFailure.PairingSessionUnavailable
            RejectionReason.PairingSessionExpired -> RemoteFailure.PairingSessionExpired
            RejectionReason.PairingCodeMismatch -> RemoteFailure.PairingCodeRejected
            RejectionReason.MissingOperationIdentifier -> RemoteFailure.MissingOperationIdentifier
        }

    /** 映射插件的执行失败。 */
    fun fromFailed(error: RemoteError): RemoteFailure =
        when (error.code) {
            RemoteErrorCode.InternalFailure -> RemoteFailure.ServerInternalFailure
            RemoteErrorCode.BurpRuntimeFailure -> RemoteFailure.BurpRuntimeFailure
            RemoteErrorCode.SerializationFailure -> RemoteFailure.ServerSerializationFailure
            RemoteErrorCode.Timeout -> RemoteFailure.TimedOut
            RemoteErrorCode.NotImplemented -> RemoteFailure.ActionNotSupported
            RemoteErrorCode.RequestPayloadTooLarge -> RemoteFailure.PayloadTooLarge
        }

    /**
     * 映射传输级 HTTP 状态码。
     *
     * 插件只在传输级问题上用状态码，领域失败一律放在应答体里，因此这里遇到的多半是契约外的应答。
     */
    fun fromHttpStatusCode(statusCode: Int): RemoteFailure =
        when {
            statusCode == PAYLOAD_TOO_LARGE_STATUS_CODE -> RemoteFailure.PayloadTooLarge
            statusCode in SERVER_ERROR_STATUS_CODES -> RemoteFailure.ServerInternalFailure
            else -> RemoteFailure.MalformedServerResponse
        }

    // 插件对超长请求体回 413，与应答体里的 request_payload_too_large 是同一件事的两个出口。
    private const val PAYLOAD_TOO_LARGE_STATUS_CODE = 413

    private val SERVER_ERROR_STATUS_CODES = 500..599
}
