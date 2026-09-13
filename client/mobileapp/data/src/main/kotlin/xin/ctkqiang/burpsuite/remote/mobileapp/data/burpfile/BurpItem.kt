package xin.ctkqiang.burpsuite.remote.mobileapp.data.burpfile

/**
 * 一条可在 Burp 与其它工具之间交换的报文。
 *
 * 字段集对齐 Burp「Export items」写出的那个形状，而不是我们自己的领域模型：
 * 这个类型的唯一用途是落到文件里、再被别人读回来，因此它必须长成对面认得的样子。
 *
 * [requestText] 与 [responseText] 是**完整的 HTTP 报文文本**（起始行 + 头 + 空行 + 体），
 * 不是插件那种「头一段、体一段」的分开形式：Burp 的 item 里 request 就是一段能直接
 * 丢进 Repeater 的原文，分开存反而要求每个读的人自己拼。
 *
 * 端口与协议在这里是必填的：Burp 的 item 靠 url/host/port/protocol 四项定位，
 * 缺了它们导入回去会变成一条指向错误目标的记录，那比不导出更糟。
 *
 * @property url 完整 URL。
 * @property host 主机名。
 * @property port 端口。
 * @property protocol 协议，取 http 或 https。
 * @property method 请求方法。
 * @property path 路径（含查询串，不含主机）。
 * @property statusCode 响应状态码；响应未到达时为 0。
 * @property responseLength 响应字节数；未知时为 0。
 * @property mimeType MIME 类型；未知时为空串。
 * @property requestText 完整请求报文。
 * @property responseText 完整响应报文；响应未到达时为空串。
 */
data class BurpItem(
    val url: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val method: String,
    val path: String,
    val statusCode: Int,
    val responseLength: Int,
    val mimeType: String,
    val requestText: String,
    val responseText: String,
)
