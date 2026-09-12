package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.AggregateType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.EventEnvelope
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.AggregateTypeName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.JournalEvent
import java.nio.charset.StandardCharsets

/**
 * 线上事件信封 → 日志事件。
 *
 * 只做形状翻译，不判断「认不认识这条事件」：认不认识是投影层的事，日志要原样收下事实。
 */
object RemoteJournalEventMapper {
    /** 映射一条事件；同一信封反复映射得到同样的结果，重放才有唯一依据。 */
    fun map(envelope: EventEnvelope): JournalEvent =
        JournalEvent(
            eventIdentifier = envelope.eventIdentifier,
            sequenceNumber = envelope.sequenceNumber,
            aggregateType = AggregateTypeName(aggregateTypeNameOf(envelope.aggregateType)),
            aggregateIdentifier = envelope.aggregateIdentifier,
            eventType = envelope.eventType,
            eventVersion = DEFAULT_EVENT_VERSION,
            occurredAt = envelope.occurredAt,
            payload = encodePayload(envelope.payload),
            // 线上信封目前不带元数据列；将来插件加上时，这里再加映射。
            metadata = emptyMap(),
        )

    // 聚合类型在领域侧是文本而不是枚举（新版插件可能引入客户端不认识的类型），因此把枚举还原成线上写法。
    private fun aggregateTypeNameOf(aggregateType: AggregateType): String =
        RemoteWireJson.instance.encodeToJsonElement(AggregateType.serializer(), aggregateType).jsonPrimitive.content

    private fun encodePayload(payload: JsonObject): ByteArray =
        RemoteWireJson.instance.encodeToString(JsonObject.serializer(), payload).toByteArray(StandardCharsets.UTF_8)

    // 线上信封未携带载荷版本，当前协议只有第 1 版可用。
    private const val DEFAULT_EVENT_VERSION = 1
}
