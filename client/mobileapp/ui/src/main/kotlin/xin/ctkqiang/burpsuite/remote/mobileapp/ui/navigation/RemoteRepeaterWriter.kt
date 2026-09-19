package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult

/**
 * 把一条原始 HTTP 请求文本推送到 Repeater 的写入端口。
 *
 * 与 [RemoteHistoryScopeWriter] 同源：端口由界面层声明、在装配层实现，因为连接配置只活在本机
 * 偏好里，而那份偏好的读端口住在数据层，界面层看不到数据层（feature:history 只依赖 :domain 与 :ui）。
 *
 * 传的是 [requestText]——完整的原始 HTTP 请求文本（请求行 + 头部 + 空行 + 可选 body），
 * 不是标识符：插件端的 Repeater create endpoint 拿到 requestText 后用 `HttpRequest.httpRequest()`
 * 重新解析并存储。之所以不在这一层用标识符去查，是因为插件端虽然存了历史，但 Montoya API
 * 不给读 Burp PC Repeater tab 里已有的条目——只能由客户端把请求文本本身递过去。
 *
 * @property requestText 完整 HTTP 请求文本，会被插件的 `HttpRequest.httpRequest()` 解析。
 * @property tabName 可选 tab 名；传 null 时插件用默认 tab。
 */
fun interface RemoteRepeaterWriter {
    /**
     * 把 [requestText] 推送到插件端的 Repeater store。
     *
     * 结局由 [RemoteResult] 原样带出，界面据此把「没配对」「连不上」「插件还没做」分开说。
     */
    suspend fun sendToRepeater(
        requestText: String,
        tabName: String?,
    ): RemoteResult<Unit>
}
