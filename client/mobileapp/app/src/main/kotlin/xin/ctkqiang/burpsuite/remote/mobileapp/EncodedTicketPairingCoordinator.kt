package xin.ctkqiang.burpsuite.remote.mobileapp

import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicketDecoder
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.PairingAttempt
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingConclusion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingCoordinator

/**
 * 配对入口的实现。
 *
 * 校验尽量在本地做完再发请求：票据过期这类结论插件必然也会拒，但等一个来回再告诉用户
 * 「其实这张票早就过期了」没有任何好处（plan §55）。
 */
class EncodedTicketPairingCoordinator(
    private val remoteControlClient: RemoteControlClient,
    private val timeProvider: TimeProvider,
    private val technicalLog: TechnicalLog,
) : RemotePairingCoordinator {
    override suspend fun pairWithEncodedTicket(encodedTicketText: String): RemotePairingConclusion {
        val ticket = decodeTicket(encodedTicketText) ?: return RemotePairingConclusion.MalformedTicket

        if (ticket.protocolVersion != RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION) {
            technicalLog.record(
                TechnicalLogEvent(
                    category = TechnicalLogCategory.Failure,
                    message = "票据协议版本与客户端不一致，本地拦下",
                    attributes =
                        mapOf(
                            "ticketProtocolVersion" to ticket.protocolVersion.toString(),
                            "clientProtocolVersion" to RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION.toString(),
                        ),
                ),
            )
            return RemotePairingConclusion.ProtocolVersionUnsupported
        }

        if (!ticket.expiresAt.isAfter(timeProvider.now())) {
            technicalLog.record(
                TechnicalLogEvent(
                    category = TechnicalLogCategory.Failure,
                    message = "票据已过期，本地拦下，未发给插件",
                ),
            )
            return RemotePairingConclusion.TicketExpired
        }

        val result =
            remoteControlClient.pair(
                PairingAttempt(
                    host = ticket.host,
                    port = ticket.port,
                    challengeIdentifier = ticket.challengeIdentifier,
                    pairingCode = ticket.pairingCode,
                ),
            )

        return when (result) {
            is RemoteResult.Succeeded -> RemotePairingConclusion.Succeeded
            is RemoteResult.Failed -> conclusionFor(result.failure)
        }
    }

    // 二维码里可能是任意一张名片或一条链接；解不出来是正常结果，不是故障。
    private fun decodeTicket(encodedTicketText: String): PairingTicket? =
        try {
            PairingTicketDecoder.decodeFromText(encodedTicketText)
        } catch (malformedTicket: IllegalArgumentException) {
            technicalLog.record(
                TechnicalLogEvent(
                    category = TechnicalLogCategory.Pairing,
                    message = "文本不是配对票据；不记原文，二维码内容属于用户数据",
                    failure = malformedTicket,
                ),
            )
            null
        }

    private fun conclusionFor(failure: RemoteFailure): RemotePairingConclusion =
        when (failure) {
            RemoteFailure.DeviceNotPaired -> RemotePairingConclusion.DeviceNotPaired
            RemoteFailure.PairingCodeRejected -> RemotePairingConclusion.PairingCodeMismatch

            RemoteFailure.PairingSessionExpired,
            RemoteFailure.PairingSessionUnavailable,
            -> RemotePairingConclusion.PairingSessionExpired

            RemoteFailure.ProtocolVersionUnsupported -> RemotePairingConclusion.ProtocolVersionUnsupported

            RemoteFailure.ServerUnavailable,
            RemoteFailure.TransportFailure,
            RemoteFailure.TimedOut,
            -> RemotePairingConclusion.Unreachable

            else -> RemotePairingConclusion.Failed
        }
}
