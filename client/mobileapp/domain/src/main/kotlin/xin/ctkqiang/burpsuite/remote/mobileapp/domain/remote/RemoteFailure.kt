package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 远程调用的领域错误。
 *
 * 用封闭类型而不是异常（rules.md §7.3）：插件明确报「尚未实现」也好、链路断了也好，
 * 都是预期内的结局，界面据此说实话，而不是把栈信息翻译成人话。
 */
sealed interface RemoteFailure {
    /** 端点不可达。 */
    data object ServerUnavailable : RemoteFailure

    /** 请求或握手超时。 */
    data object TimedOut : RemoteFailure

    /** 传输层故障，连不上也读不出。 */
    data object TransportFailure : RemoteFailure

    /** 插件回了不符合契约的报文。 */
    data object MalformedServerResponse : RemoteFailure

    /** 认证被拒；设备身份不被承认。 */
    data object AuthenticationRejected : RemoteFailure

    /** 设备尚未配对。 */
    data object DeviceNotPaired : RemoteFailure

    /** 设备权限不足。 */
    data object DeviceNotAuthorized : RemoteFailure

    /** 超出插件侧的速率限制。 */
    data object RateLimited : RemoteFailure

    /** 目标不可用。 */
    data object TargetUnavailable : RemoteFailure

    /** 协议版本不受支持。 */
    data object ProtocolVersionUnsupported : RemoteFailure

    /** 配对会话不存在。 */
    data object PairingSessionUnavailable : RemoteFailure

    /** 配对会话已过期。 */
    data object PairingSessionExpired : RemoteFailure

    /** 配对码不匹配。 */
    data object PairingCodeRejected : RemoteFailure

    /** 控制命令缺少操作标识，插件无法保证幂等，只能拒绝。 */
    data object MissingOperationIdentifier : RemoteFailure

    /** 该能力插件尚未实现；如实上报缺口，好过让上层拿到编造的数据。 */
    data object ActionNotSupported : RemoteFailure

    /** 请求体超过插件允许的大小。 */
    data object PayloadTooLarge : RemoteFailure

    /** 插件内部故障。 */
    data object ServerInternalFailure : RemoteFailure

    /** Burp 运行时故障。 */
    data object BurpRuntimeFailure : RemoteFailure

    /** 插件序列化故障。 */
    data object ServerSerializationFailure : RemoteFailure

    /** 需要重新取快照，但取快照这一步也没成功。 */
    data object ResynchronisationFailed : RemoteFailure
}
