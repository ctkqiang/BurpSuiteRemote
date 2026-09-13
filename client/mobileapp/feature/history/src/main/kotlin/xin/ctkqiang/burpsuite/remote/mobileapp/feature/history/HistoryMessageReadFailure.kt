package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure

/**
 * 报文本体读取失败的归类。
 *
 * 领域层的 [RemoteFailure] 有二十来种取值，全都翻成界面文案既没有意义也翻译不完；
 * 这里按「用户下一步要做什么」归成几类，归类发生在 ViewModel，界面只负责取文案。
 *
 * 分成这几类而不是统一成一句「读取失败」，是因为每一种的下一步都不同：没配对要去连接页，
 * 连不上要查网络与插件是否在跑，插件不支持就是真的没有这条通路，记录不存在则根本不该重试。
 *
 * @property messageResource 这类失败对用户说的一句话。
 * @property isRetryable 再取一次是否有意义；没有意义时界面不再给重试按钮，免得用户白按。
 */
enum class HistoryMessageReadFailure(
    @StringRes val messageResource: Int,
    val isRetryable: Boolean,
) {
    /** 本机与插件的配对状态不可用；下一步是去 Burp 连接页重新配对。 */
    NotPaired(
        messageResource = R.string.history_detail_message_failure_not_paired,
        isRetryable = true,
    ),

    /** 地址端口可达性有问题：插件没在跑，或者网络不通。 */
    Unreachable(
        messageResource = R.string.history_detail_message_failure_unreachable,
        isRetryable = true,
    ),

    /** 插件这一版不提供报文本体，重试多少次都一样。 */
    NotSupported(
        messageResource = R.string.history_detail_message_failure_not_supported,
        isRetryable = false,
    ),

    /** 插件在站点地图里找不到这条记录；不重试，因为它已经不在那里了。 */
    RecordMissing(
        messageResource = R.string.history_detail_message_failure_record_missing,
        isRetryable = false,
    ),

    /** 应答读不懂：契约不一致，属于需要人看的故障。 */
    MalformedResponse(
        messageResource = R.string.history_detail_message_failure_malformed,
        isRetryable = false,
    ),

    /** 插件明确拒绝了这次读取，但没给出更细的类别。 */
    Refused(
        messageResource = R.string.history_detail_message_failure_refused,
        isRetryable = true,
    ),
    ;

    companion object {
        /**
         * 把领域层的失败结局归到本枚举的一类。
         *
         * 这里对 [RemoteFailure] 做穷尽匹配而不是留一个 `else`：将来插件新增一种失败结局时，
         * 编译器会在这里拦下来，逼着人决定它该归到哪一类，而不是静默落进「未知」。
         */
        fun of(failure: RemoteFailure): HistoryMessageReadFailure =
            when (failure) {
                RemoteFailure.DeviceNotPaired,
                RemoteFailure.DeviceNotAuthorized,
                RemoteFailure.AuthenticationRejected,
                RemoteFailure.PairingSessionUnavailable,
                RemoteFailure.PairingSessionExpired,
                RemoteFailure.PairingCodeRejected,
                -> NotPaired

                RemoteFailure.ServerUnavailable,
                RemoteFailure.TimedOut,
                RemoteFailure.TransportFailure,
                RemoteFailure.ResynchronisationFailed,
                -> Unreachable

                RemoteFailure.ActionNotSupported,
                RemoteFailure.ProtocolVersionUnsupported,
                RemoteFailure.MissingOperationIdentifier,
                -> NotSupported

                RemoteFailure.BurpRuntimeFailure,
                RemoteFailure.TargetUnavailable,
                -> RecordMissing

                RemoteFailure.MalformedServerResponse,
                RemoteFailure.ServerSerializationFailure,
                -> MalformedResponse

                RemoteFailure.PayloadTooLarge,
                RemoteFailure.RateLimited,
                RemoteFailure.ServerInternalFailure,
                -> Refused
            }
    }
}
