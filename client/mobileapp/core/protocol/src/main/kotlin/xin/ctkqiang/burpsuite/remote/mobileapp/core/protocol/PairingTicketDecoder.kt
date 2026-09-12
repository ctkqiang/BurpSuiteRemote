// 二维码文本到配对票据的解码入口。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.json.Json

/** 把二维码里的文本解成配对票据；票据是客户端拿到地址、配对码与会话身份的唯一来源。 */
object PairingTicketDecoder {
    // 不开启宽松解析：两端都严格按契约来，解不出来就是契约不一致，不能靠猜补上。
    private val json = Json

    /** 解码票据；文本不合契约就抛异常，绝不返回一张被默认值补齐的票据。 */
    fun decodeFromText(encodedTicketText: String): PairingTicket =
        json.decodeFromString(PairingTicket.serializer(), encodedTicketText)
}
