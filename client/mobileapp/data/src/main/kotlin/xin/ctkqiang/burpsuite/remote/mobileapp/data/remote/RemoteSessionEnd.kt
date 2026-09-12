package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

/** 事件通道一次会话的结束原因；连接层据此决定是退避重连还是停下并如实上报状态。 */
enum class RemoteSessionEnd {
    /** 插件或网络关闭了会话。 */
    PeerClosed,

    /** 本地事件序号出现断洞，必须带着新的基准重新握手续传（rules.md §5.5）。 */
    ResumeRequired,

    /** 插件拒绝了这个设备身份。 */
    AuthenticationRejected,

    /** 双方线上契约不一致。 */
    ProtocolMismatch,

    /** 握手在超时之前没有完成。 */
    HandshakeTimedOut,

    /** 端点不可达。 */
    ServerUnavailable,

    /** 传输层故障；与不可达相比，链路曾经建立过。 */
    TransportFailure,

    /** 需要快照，但取快照本身也失败了。 */
    ResynchronisationFailed,
}
