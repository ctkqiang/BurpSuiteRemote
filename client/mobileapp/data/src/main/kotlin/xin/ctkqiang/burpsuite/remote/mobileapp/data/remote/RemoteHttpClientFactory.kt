package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json

/**
 * 传输层 HttpClient 的唯一构造处。
 *
 * 引擎由调用方注入：默认 OkHttp，测试传 MockEngine，装配层不必知道 JSON 与 WebSocket 插件是怎么装的。
 * TLS 尚未实现——插件侧当前只提供明文端点，改用 https/wss 方案不等于链路已经加密（plan §54）。
 */
object RemoteHttpClientFactory {
    /** 造一个装上 JSON 协商与 WebSocket 插件的客户端。 */
    fun create(engine: HttpClientEngine = OkHttp.create {}): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(RemoteWireJson.instance)
            }
            install(WebSockets)
        }
}
