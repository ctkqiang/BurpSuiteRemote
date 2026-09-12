// 配对票据的线上文本形式。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.json.Json

/** 把配对票据编码成写进二维码的文本；用 JSON，是为了加字段时不用自己设计分隔协议。 */
object PairingTicketEncoder {
    // 不开启宽松解析，两端都严格按契约来。
    private val json = Json

    /** 编码成单行 JSON；二维码按字节算容量，多一个换行就少一分余量。 */
    fun encodeToText(pairingTicket: PairingTicket): String =
        json.encodeToString(PairingTicket.serializer(), pairingTicket)
}
