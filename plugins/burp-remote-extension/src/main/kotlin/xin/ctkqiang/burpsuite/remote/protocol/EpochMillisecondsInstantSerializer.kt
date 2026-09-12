// 时间点的线上编码：自 Unix 纪元起的毫秒整数。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

/** 时间点按自 Unix 纪元起的毫秒整数编解码；契约要求是数字，不是 ISO 8601 文本。 */
object EpochMillisecondsInstantSerializer : KSerializer<Instant> {
    /** 声明成长整数原语，生成的 JSON 里该字段才是数字。 */
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EpochMillisecondsInstant", PrimitiveKind.LONG)

    /** 按契约写成毫秒整数。 */
    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeLong(value.toEpochMilli())
    }

    /** 读回时间点；解不出来就抛，不能悄悄换成当前时间，否则会凭空造出错误时间戳。 */
    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}
