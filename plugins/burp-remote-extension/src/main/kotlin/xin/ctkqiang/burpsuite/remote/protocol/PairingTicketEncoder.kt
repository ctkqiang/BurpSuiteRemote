/**
 * Burp Remote —— 协议层 / 配对
 *
 * 声明配对票据的线上文本形式。票据最终会被编码进二维码，而二维码里的内容同样是线上
 * 契约的一部分，因此编码规则必须留在协议层，不能散落在界面代码里。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.json.Json

/**
 * 把配对票据编码为写入二维码的文本。
 *
 * 选择 JSON 而不是自定义的分隔符格式：两端都由 kotlinx.serialization 生成与解析，字段
 * 增删时无需自行设计版本化的分隔协议；而自定义格式一旦要增加字段，就必须自己处理
 * 「旧客户端遇到多余字段」的问题，那正是协议兼容缺陷的高发地。
 *
 * 编码结果不含换行——二维码容量按字节计算，多一个换行就少一分余量。
 */
object PairingTicketEncoder {
    /**
     * 票据的 JSON 编解码器。
     *
     * 不开启任何宽松选项：票据由插件生成、由移动端解析，两端都必须严格按契约处理。
     * 任何「尽力解析」都会把协议不匹配伪装成数据缺失，让两端版本不一致这件事拖到
     * 更靠后的环节才暴露。
     */
    private val json = Json

    /**
     * 把票据编码为单行 JSON 文本。
     *
     * @param pairingTicket 待编码的配对票据。
     * @return 可直接交给二维码编码器的文本。
     */
    fun encodeToText(pairingTicket: PairingTicket): String =
        json.encodeToString(PairingTicket.serializer(), pairingTicket)
}
