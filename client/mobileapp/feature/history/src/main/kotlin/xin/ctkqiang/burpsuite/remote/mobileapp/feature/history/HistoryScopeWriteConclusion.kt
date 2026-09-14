package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure

/**
 * 一次「加入作用域」尝试的结论（与配对结论同一套写法：枚举成员配一句给用户看的话）。
 *
 * 结论分得这么细，是因为每一种都要给出不同的下一步：没配对要去连接页重新配对，连不上要查网络与
 * 插件是否在跑，插件这一版没做该端点就是真的没有这条通路，记录已不在插件里则根本不该再试。
 * 统一成一句「加入失败」等于把排查工作全丢给用户。
 *
 * [WriterUnavailable] 与 [NotSupported] 是两件事，不能合并：前者是客户端这一侧根本没接上写入端口
 * （界面装配的问题），后者是插件那一侧没有这个端点（版本的问题）。说成同一句会让用户去升级一个
 * 本来就够新的插件，方向全错。
 *
 * @property messageResource 这个结论说给用户听的一句话。
 */
enum class HistoryScopeWriteConclusion(
    @StringRes val messageResource: Int,
) {
    /** 插件已把这条记录的主机写进作用域。 */
    Added(R.string.history_detail_scope_conclusion_added),

    /** 本机与插件的配对状态不可用；下一步是去 Burp 连接页重新配对。 */
    NotPaired(R.string.history_detail_scope_conclusion_not_paired),

    /** 地址端口可达性有问题：插件没在跑，或者网络不通。 */
    Unreachable(R.string.history_detail_scope_conclusion_unreachable),

    /** 插件这一版不提供作用域端点，重试多少次都一样。 */
    NotSupported(R.string.history_detail_scope_conclusion_not_supported),

    /** 客户端这一侧没有接上作用域写入端口；属于装配缺口，不是插件的问题。 */
    WriterUnavailable(R.string.history_detail_scope_conclusion_writer_unavailable),

    /** 插件在站点地图里找不到这条记录；不重试，因为它已经不在那里了。 */
    RecordMissing(R.string.history_detail_scope_conclusion_record_missing),

    /** 应答读不懂：契约不一致，属于需要人看的故障。 */
    MalformedResponse(R.string.history_detail_scope_conclusion_malformed),

    /** 插件明确拒绝了这次写入，但没给出更细的类别。 */
    Refused(R.string.history_detail_scope_conclusion_refused),
    ;

    companion object {
        /**
         * 把领域层的失败结局归到本枚举的一类。
         *
         * 这里对 [RemoteFailure] 做穷尽匹配而不是留一个 `else`：将来插件新增一种失败结局时，
         * 编译器会在这里拦下来，逼着人决定它该归到哪一类，而不是静默落进「未知」。
         */
        fun of(failure: RemoteFailure): HistoryScopeWriteConclusion =
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
