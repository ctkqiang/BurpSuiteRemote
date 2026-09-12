// Montoya 版历史增长信号实现。

package xin.ctkqiang.burpsuite.remote.adapter

import burp.api.montoya.core.Registration
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.Http
import burp.api.montoya.http.handler.HttpHandler
import burp.api.montoya.http.handler.HttpRequestToBeSent
import burp.api.montoya.http.handler.HttpResponseReceived
import burp.api.montoya.http.handler.RequestToBeSentAction
import burp.api.montoya.http.handler.ResponseReceivedAction

/**
 * 用 Montoya 的 HTTP 处理钩子实现历史信号，只观察、不改写报文。
 *
 * Montoya 2025.5 没有代理历史变更监听器，只能借「Burp 刚处理完一次代理交互」当信号；原样 continueWith 放行，
 * 于是既不改写报文，也不越过操作者自己的代理拦截开关。
 */
class MontoyaProxyHistorySignalSource(private val http: Http) : BurpProxyHistorySignalSource {
    override fun subscribeToHistorySignals(onHistoryMayHaveGrown: () -> Unit): BurpHistorySignalSubscription {
        val registration =
            http.registerHttpHandler(
                object : HttpHandler {
                    override fun handleHttpRequestToBeSent(
                        requestToBeSent: HttpRequestToBeSent,
                    ): RequestToBeSentAction =
                        RequestToBeSentAction.continueWith(requestToBeSent, requestToBeSent.annotations())

                    override fun handleHttpResponseReceived(
                        responseReceived: HttpResponseReceived,
                    ): ResponseReceivedAction {
                        // 只有代理流量才会落进代理历史，其他工具的信号只会白白拉一遍历史。
                        if (responseReceived.toolSource().isFromTool(ToolType.PROXY)) {
                            onHistoryMayHaveGrown()
                        }
                        return ResponseReceivedAction.continueWith(responseReceived, responseReceived.annotations())
                    }
                },
            )
        return MontoyaHistorySignalSubscription(registration)
    }
}

private class MontoyaHistorySignalSubscription(
    private val registration: Registration,
) : BurpHistorySignalSubscription {
    override fun cancel() {
        // 已摘掉的句柄再摘一次会抛异常，而卸载路径上重复取消是常态。
        if (registration.isRegistered()) {
            registration.deregister()
        }
    }
}
