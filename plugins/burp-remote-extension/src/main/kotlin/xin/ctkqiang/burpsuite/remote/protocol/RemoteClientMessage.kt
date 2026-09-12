// 客户端发往插件的报文。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 客户端发来的报文。
 *
 * 握手（连接、认证、续传）与配对共用同一个报文形状：字段按 [messageType] 取用，未用到的字段为 null。
 * 合成一个类型而不是每类消息一个类型，是为了让「消息类别 + 协议版本」这条公共契约只有一处。
 *
 * @property protocolVersion 客户端使用的协议版本。
 * @property messageType 消息类别，决定其余字段的含义。
 * @property deviceIdentifier 认证时提交的设备身份；配对时为空。
 * @property sequenceNumber 续传时声明的「已收到的最后一个序号」。
 * @property challengeIdentifier 配对时提交的会话标识。
 * @property pairingCode 配对时提交的一次性配对码。
 */
@Serializable
data class RemoteClientMessage(
    val protocolVersion: Int,
    val messageType: RemoteMessageType,
    val deviceIdentifier: DeviceIdentifier? = null,
    val sequenceNumber: Long? = null,
    val challengeIdentifier: PairingChallengeIdentifier? = null,
    val pairingCode: PairingCode? = null,
)
