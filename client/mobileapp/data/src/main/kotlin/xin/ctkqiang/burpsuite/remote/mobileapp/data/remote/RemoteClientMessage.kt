package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.DeviceIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.PairingChallengeIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.PairingCodeSerializer

/**
 * 客户端发往插件的报文；与插件侧 RemoteClientMessage 同形，字段按 [messageType] 取用。
 *
 * 握手（连接、认证、续传）与配对共用一个形状，是为了让「消息类别 + 协议版本」这条公共契约只有一处。
 *
 * @property protocolVersion 客户端使用的协议版本。
 * @property messageType 消息类别。
 * @property deviceIdentifier 认证时提交的设备身份；配对时为空。
 * @property sequenceNumber 续传时声明的「已收到的最后一个序号」。
 * @property challengeIdentifier 配对时提交的会话标识。
 * @property pairingCode 配对时提交的一次性配对码。
 */
@Serializable
data class RemoteClientMessage(
    val protocolVersion: Int,
    val messageType: RemoteClientMessageType,
    @Serializable(with = DeviceIdentifierSerializer::class)
    val deviceIdentifier: DeviceIdentifier? = null,
    val sequenceNumber: Long? = null,
    @Serializable(with = PairingChallengeIdentifierSerializer::class)
    val challengeIdentifier: PairingChallengeIdentifier? = null,
    @Serializable(with = PairingCodeSerializer::class)
    val pairingCode: PairingCode? = null,
)
