// 配对码在线上是裸字符串。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode

/**
 * [PairingCode] 的线上形式：裸字符串。
 *
 * 身份类型住在 :core:model 且不带注解（rules.md §6.1），所以序列化器留在协议模块，由 DTO 显式指定。
 */
object PairingCodeSerializer : KSerializer<PairingCode> {
    /** 声明成字符串原语，生成的 JSON 里该字段才是裸字符串。 */
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PairingCode", PrimitiveKind.STRING)

    /** 按契约写成裸字符串。 */
    override fun serialize(
        encoder: Encoder,
        value: PairingCode,
    ) {
        encoder.encodeString(value.value)
    }

    /** 读回配对码；解不出来就抛，不悄悄造一个空码。 */
    override fun deserialize(decoder: Decoder): PairingCode = PairingCode(decoder.decodeString())
}
