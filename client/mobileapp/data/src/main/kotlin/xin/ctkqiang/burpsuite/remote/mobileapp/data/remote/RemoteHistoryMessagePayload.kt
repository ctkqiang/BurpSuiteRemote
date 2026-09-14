package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.Serializable

/**
 * /v1/history/{historyIdentifier} 的载荷形状。
 *
 * 字段集与插件侧 `BurpHistoryAdapter` 的构造函数一一对应：插件只发这九项，客户端因此不替它多编字段
 * （rules.md §11）。四个文本项带默认值、状态码也可为空，是因为插件在响应未到达时发的是 JSON null——
 * 少一个容得下 null 的默认值就会把「没有这段字节」解析成契约错误，那是把正常结局当故障。
 *
 * @property historyIdentifier 插件为这条记录算出的稳定标识。
 * @property method 请求方法。
 * @property host 请求的主机名。
 * @property path 请求路径。
 * @property status 响应状态码；插件侧字段名就是 status。
 * @property requestHeaders 请求头原文；插件未捕获到时为空。
 * @property requestBody 请求体原文；插件未捕获到时为空。
 * @property responseHeaders 响应头原文；响应未到达时为空。
 * @property responseBody 响应体原文；响应未到达时为空。
 */
@Serializable
data class RemoteHistoryMessagePayload(
    val historyIdentifier: String,
    val method: String,
    val host: String,
    val path: String,
    val status: Int? = null,
    val requestHeaders: String? = null,
    val requestBody: String? = null,
    val responseHeaders: String? = null,
    val responseBody: String? = null,
)
