package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

/** 事件流阶段收到一条控制信号之后的去向。 */
internal sealed interface RemoteSignalDisposition {
    /** 信号与本地状态一致，继续收事件。 */
    data object Continue : RemoteSignalDisposition

    /** 插件已不保留所需序号区间：先取快照，再带着新基准续传（plan §10）。 */
    data object ResynchroniseThenResume : RemoteSignalDisposition

    /** 会话就此结束，结局由 [sessionEnd] 给出。 */
    data class EndSession(val sessionEnd: RemoteSessionEnd) : RemoteSignalDisposition

    companion object {
        /** 由信号码判定去向；六种信号逐一列出，插件新增信号码时这里会先编译不过。 */
        fun forSignalCode(code: RemoteConnectionSignalCode): RemoteSignalDisposition =
            when (code) {
                RemoteConnectionSignalCode.AuthenticationSucceeded -> Continue

                RemoteConnectionSignalCode.EventsNoLongerAvailable,
                RemoteConnectionSignalCode.SnapshotRequired,
                -> ResynchroniseThenResume

                RemoteConnectionSignalCode.AuthenticationFailed,
                RemoteConnectionSignalCode.AuthenticationRequired,
                -> EndSession(RemoteSessionEnd.AuthenticationRejected)

                RemoteConnectionSignalCode.ProtocolVersionUnsupported ->
                    EndSession(RemoteSessionEnd.ProtocolMismatch)
            }
    }
}
