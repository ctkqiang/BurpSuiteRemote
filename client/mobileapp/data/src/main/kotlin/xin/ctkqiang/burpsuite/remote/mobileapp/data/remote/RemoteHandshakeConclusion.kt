package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

/**
 * 事件通道握手阶段的结论。
 *
 * 判定抽成纯函数：握手只有几步，但每步的分支都只取决于已解码的报文；把它留在 WebSocket 回调里，
 * 分支就只能靠真起一条连接才测得到。
 */
internal sealed interface RemoteHandshakeConclusion {
    /** 认证通过，可以提交续传请求。 */
    data object Authenticated : RemoteHandshakeConclusion

    /** 设备身份被拒；重连不会变好。 */
    data object AuthenticationRejected : RemoteHandshakeConclusion

    /** 线上契约不一致。 */
    data object ProtocolMismatch : RemoteHandshakeConclusion

    /** 对端在握手完成之前关闭了会话。 */
    data object PeerClosed : RemoteHandshakeConclusion

    companion object {
        /** 由一条已解码报文判定结论；返回 null 表示这条报文不构成结论，继续收。 */
        fun forInboundMessage(message: RemoteInboundMessage): RemoteHandshakeConclusion? =
            when (message) {
                is RemoteInboundMessage.SignalReceived -> forSignalCode(message.code)

                // 认证之前插件不推事件；真收到了也不构成握手结论，按噪声忽略。
                is RemoteInboundMessage.EventReceived -> null

                // 解不出类别的报文按契约不一致收场，绝不猜它的含义。
                RemoteInboundMessage.Malformed -> ProtocolMismatch
            }

        private fun forSignalCode(code: RemoteConnectionSignalCode): RemoteHandshakeConclusion? =
            when (code) {
                RemoteConnectionSignalCode.AuthenticationSucceeded -> Authenticated

                RemoteConnectionSignalCode.AuthenticationFailed,
                RemoteConnectionSignalCode.AuthenticationRequired,
                -> AuthenticationRejected

                RemoteConnectionSignalCode.ProtocolVersionUnsupported -> ProtocolMismatch

                // 区间不可用与要快照说的是续传阶段的事，握手还没结束时它们不改变会话方向。
                RemoteConnectionSignalCode.EventsNoLongerAvailable,
                RemoteConnectionSignalCode.SnapshotRequired,
                -> null
            }
    }
}
