// 事件类型串在线上是裸字符串。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType

/**
 * [EventType] 的线上形式：裸字符串。
 *
 * 不认识的类型串会被原样读入而不是拒绝，否则新版插件新增的事件会被整条丢掉（见 EventType 的说明）。
 */
object EventTypeSerializer : KSerializer<EventType> {
    /** 声明成字符串原语，生成的 JSON 里该字段才是裸字符串。 */
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EventType", PrimitiveKind.STRING)

    /** 按契约写成裸字符串。 */
    override fun serialize(
        encoder: Encoder,
        value: EventType,
    ) {
        encoder.encodeString(value.value)
    }

    /** 读回类型串；不认识也照样返回，把「认不认识」留给投影层判断。 */
    override fun deserialize(decoder: Decoder): EventType = EventType(decoder.decodeString())
}
