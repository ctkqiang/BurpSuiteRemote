package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.EventEnvelope

/** 从事件通道读到的一条报文。 */
sealed interface RemoteInboundMessage {
    /**
     * 一条事件。
     *
     * @property envelope 事件的线上信封。
     */
    data class EventReceived(val envelope: EventEnvelope) : RemoteInboundMessage

    /**
     * 一条控制信号。
     *
     * @property code 信号码。
     */
    data class SignalReceived(val code: RemoteConnectionSignalCode) : RemoteInboundMessage

    /** 报文不符合契约；不猜它的含义，交由连接层按协议不一致处理。 */
    data object Malformed : RemoteInboundMessage
}
