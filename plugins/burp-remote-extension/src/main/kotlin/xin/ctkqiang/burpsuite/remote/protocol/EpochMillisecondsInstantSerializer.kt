/**
 * Burp Remote —— 协议层 / 序列化
 *
 * 声明时间点的线上编码方式。协议层内部继续使用语义明确的时间点类型，线路上则严格遵循
 * 契约所约定的毫秒整数，两者之间的落差由本文件负责抹平。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

/**
 * 把时间点编解码为「自 Unix 纪元起的毫秒整数」。
 *
 * 协议契约规定 `occurredAt` 是毫秒整数而不是 ISO 8601 文本，因此这里不能沿用任何库默认的
 * 时间序列化器。坚持使用整数是有实际理由的：事件信封会被大量写入本地日志，并参与排序与
 * 去重，整数比较既没有时区歧义也没有解析开销；而文本形式一旦两端的时区假设或精度假设不
 * 一致，就会出现「明明是同一时刻却不相等」这类隐蔽缺陷，它不会报错，只会让去重悄悄失效。
 *
 * 解码失败时直接抛出，而不是退化成当前时间。把无法识别的时间戳静默替换成「现在」，会凭空
 * 造出大量时间错误的事件，既污染排序又破坏去重，而且事后完全无法区分哪些时间是被伪造的。
 */
object EpochMillisecondsInstantSerializer : KSerializer<Instant> {
    /**
     * 序列化描述符。声明为长整数原语类型，使生成的 JSON 中该字段是数字而非字符串。
     */
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("EpochMillisecondsInstant", PrimitiveKind.LONG)

    /**
     * 把时间点写成自 Unix 纪元起的毫秒整数。
     *
     * @param encoder 目标编码器。
     * @param value 待编码的时间点。
     */
    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeLong(value.toEpochMilli())
    }

    /**
     * 从自 Unix 纪元起的毫秒整数读回时间点。
     *
     * @param decoder 来源解码器。
     * @return 解码得到的时间点。
     */
    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}
