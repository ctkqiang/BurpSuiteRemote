package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure

/**
 * 一次「送往重放」尝试的结论。
 *
 * 与 [HistoryScopeWriteConclusion] 同一套写法：枚举成员配一句给用户看的话。
 * 每种结论的下一步不同：没配对要去连接页重新配对，连不上要查网络，插件这一版没做该端点
 * 就是真的没有这条通路，请求文本拼不出来则是本体还没取回。
 *
 * [WriterUnavailable] 与 [NotSupported] 是两件事：前者是客户端这一侧没接上写入端口，
 * 后者是插件那一侧没有这个端点。
 *
 * [MessageNotLoaded] 是 Repeater 特有：推送 Repeater 需要报文本体（requestText），
 * 本体还没取回时按钮虽然可见但不可用——与 [WriterUnavailable] 分开说，因为下一步不同：
 * 前者要等本体取回或重试，后者是装配缺口。
 *
 * @property messageResource 这个结论说给用户听的一句话。
 */
enum class HistoryRepeaterConclusion(
    @StringRes val messageResource: Int,
) {
    /** 插件已把这条请求存进 Repeater store 并推到 Burp PC 的 Repeater tab。 */
    Sent(R.string.history_detail_repeater_conclusion_sent),

    /** 本机与插件的配对状态不可用；下一步是去 Burp 连接页重新配对。 */
    NotPaired(R.string.history_detail_repeater_conclusion_not_paired),

    /** 地址端口可达性有问题：插件没在跑，或者网络不通。 */
    Unreachable(R.string.history_detail_repeater_conclusion_unreachable),

    /** 插件这一版不提供 Repeater create 端点，重试多少次都一样。 */
    NotSupported(R.string.history_detail_repeater_conclusion_not_supported),

    /** 客户端这一侧没有接上 Repeater 写入端口；属于装配缺口，不是插件的问题。 */
    WriterUnavailable(R.string.history_detail_repeater_conclusion_writer_unavailable),

    /** 报文本体还没取回，无法拼出 requestText；下一步是先取回本体或重试。 */
    MessageNotLoaded(R.string.history_detail_repeater_conclusion_message_not_loaded),

    /** 应答读不懂：契约不一致，属于需要人看的故障。 */
    MalformedResponse(R.string.history_detail_repeater_conclusion_malformed),

    /** 插件明确拒绝了这次写入，但没给出更细的类别。 */
    Refused(R.string.history_detail_repeater_conclusion_refused),
    ;

    companion object {
        /**
         * 把领域层的失败结局归到本枚举的一类。
         *
         * 这里对 [RemoteFailure] 做穷尽匹配而不是留一个 `else`：将来插件新增一种失败结局时，
         * 编译器会在这里拦下来，逼着人决定它该归到哪一类。
         */
        fun of(failure: RemoteFailure): HistoryRepeaterConclusion =
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
                -> Refused

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
