package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 单条历史报文的完整内容。
 *
 * 与 `HistoryRecord` 分开：那张表只带元数据，本体按标识另行取回，列表因此不必为每一行加载大报文
 * （plan §19、plan §20）。
 *
 * 四个文本字段可为空，因为「插件没捕获到这段字节」是真实存在的结局——请求发出后连接被中断时
 * 响应头与响应体都不存在。空串与 null 必须分开：前者是「确实捕获到了，长度为零」，
 * 后者是「根本没有这段东西」，界面靠这个区别决定是画一块空代码块还是说明缺口（rules.md §5.1）。
 *
 * @property historyIdentifier 插件为这条记录算出的稳定标识。
 * @property method 请求方法。
 * @property host 请求的主机名。
 * @property path 请求路径。
 * @property statusCode 响应状态码。
 * @property requestHeaders 请求头原文；未捕获到时为空。
 * @property requestBody 请求体原文；未捕获到时为空。
 * @property responseHeaders 响应头原文；响应未到达时为空。
 * @property responseBody 响应体原文；响应未到达时为空。
 */
data class RemoteHistoryMessage(
    val historyIdentifier: String,
    val method: String,
    val host: String,
    val path: String,
    val statusCode: Int,
    val requestHeaders: String?,
    val requestBody: String?,
    val responseHeaders: String?,
    val responseBody: String?,
)
