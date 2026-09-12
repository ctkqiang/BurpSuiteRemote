package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteProtocolVersion

/** 续传报文的构造入口；序号只能来自本地日志进度，因此这里只接受它（plan §9）。 */
internal object RemoteResumeRequest {
    /** 构造一条 RESUME 报文；[sequenceNumber] 是「已收到的最后一个序号」。 */
    fun build(sequenceNumber: Long): RemoteClientMessage =
        RemoteClientMessage(
            protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
            messageType = RemoteClientMessageType.Resume,
            sequenceNumber = sequenceNumber,
        )
}
