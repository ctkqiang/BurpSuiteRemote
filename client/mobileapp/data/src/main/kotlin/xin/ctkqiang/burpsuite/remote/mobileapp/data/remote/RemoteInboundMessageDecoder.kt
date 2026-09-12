package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.EventEnvelope

/**
 * 事件通道报文的分流解码。
 *
 * 先看 messageType 再按类别解：事件与信号的字段集合没有交集，混解只会把「不认识的新报文」
 * 误判成两条已知报文中的一条。
 */
object RemoteInboundMessageDecoder {
    /** 解码一条报文；解不出具体类别时返回 [RemoteInboundMessage.Malformed]，绝不猜。 */
    fun decode(encodedText: String): RemoteInboundMessage {
        val messageElement =
            try {
                RemoteWireJson.instance.parseToJsonElement(encodedText)
            } catch (malformedText: SerializationException) {
                return RemoteInboundMessage.Malformed
            }
        val messageObject = messageElement as? JsonObject ?: return RemoteInboundMessage.Malformed

        return when (messageTypeOf(messageObject)) {
            EVENT_MESSAGE_TYPE -> decodeEvent(messageObject)
            SIGNAL_MESSAGE_TYPE -> decodeSignal(messageObject)
            else -> RemoteInboundMessage.Malformed
        }
    }

    private fun decodeEvent(messageObject: JsonObject): RemoteInboundMessage =
        try {
            RemoteInboundMessage.EventReceived(
                RemoteWireJson.instance.decodeFromJsonElement(EventEnvelope.serializer(), messageObject),
            )
        } catch (malformedEvent: IllegalArgumentException) {
            RemoteInboundMessage.Malformed
        }

    private fun decodeSignal(messageObject: JsonObject): RemoteInboundMessage =
        try {
            RemoteInboundMessage.SignalReceived(
                RemoteWireJson.instance.decodeFromJsonElement(RemoteSignalMessage.serializer(), messageObject).code,
            )
        } catch (malformedSignal: IllegalArgumentException) {
            RemoteInboundMessage.Malformed
        }

    private fun messageTypeOf(messageObject: JsonObject): String? =
        (messageObject[MESSAGE_TYPE_FIELD] as? JsonPrimitive)?.content

    private const val MESSAGE_TYPE_FIELD = "messageType"
    private const val EVENT_MESSAGE_TYPE = "event"
    private const val SIGNAL_MESSAGE_TYPE = "signal"
}
