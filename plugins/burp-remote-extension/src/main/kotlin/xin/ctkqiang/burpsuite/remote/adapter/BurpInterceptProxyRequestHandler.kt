// Montoya ProxyRequestHandler：阻塞等待手机决策，根据决策返回 continueWith 或 drop。

package xin.ctkqiang.burpsuite.remote.adapter

import burp.api.montoya.core.Annotations
import burp.api.montoya.proxy.MessageReceivedAction
import burp.api.montoya.proxy.http.InterceptedRequest
import burp.api.montoya.proxy.http.ProxyRequestHandler
import burp.api.montoya.proxy.http.ProxyRequestReceivedAction
import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import xin.ctkqiang.burpsuite.remote.protocol.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.AggregateType
import xin.ctkqiang.burpsuite.remote.protocol.EventIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RemoteEventEnvelope
import xin.ctkqiang.burpsuite.remote.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.transport.InMemoryRemoteEventStream
import java.time.Clock

/**
 * Montoya ProxyRequestHandler 实现——拦截 HTTP 请求、挂起等待手机命令、按决策返回 Burp Action。
 *
 * 关键设计：
 * - **runBlocking 阻塞 Burp 代理线程**：Montoya ProxyRequestHandler 回调是同步非 suspend 的，
 *   没有异步挂起机制；runBlocking 就是 Burp Proxy 拦截功能本身的工作方式（暂停等用户操作）。
 * - **没有活跃已配对设备时不拦截**：handler 直接返回 CONTINUE，不发事件、不登记队列，保持 Burp 原生行为不变。
 * - **60 秒硬超时**：超时默认 Forward，防止手机没反应时把代理永久卡死。
 *
 * @param interceptQueue 与 REST 端点共享的拦截队列；REST 在手机发命令时通过它解挂。
 * @param eventStream 事件通道（需要写权限，故用 [InMemoryRemoteEventStream]）：新拦截项出现时发 `intercept.created`，决策结束时发 `intercept.forwarded`/`intercept.dropped`。
 * @param hasActivePairedDevice 查询是否至少一个已连接的已配对设备；没有则 handler 放行不拦截。
 * @param clock 统一时钟；occurredAt 用它取，保证各处时间源一致。
 */
class BurpInterceptProxyRequestHandler(
    private val interceptQueue: BurpInterceptQueue,
    private val eventStream: InMemoryRemoteEventStream,
    private val hasActivePairedDevice: () -> Boolean,
    private val clock: Clock,
    private val awaitTimeoutSeconds: Long = DEFAULT_AWAIT_TIMEOUT_SECONDS,
) : ProxyRequestHandler {
    // 序号由 InMemoryRemoteEventStream 统一分配，这里不再维护计数器。

    override fun handleRequestReceived(request: InterceptedRequest): ProxyRequestReceivedAction {
        if (!hasActivePairedDevice()) {
            return ProxyRequestReceivedAction.proxyRequestReceivedAction(
                request,
                request.annotations(),
                MessageReceivedAction.CONTINUE,
            )
        }

        val wrapped =
            MontoyaInterceptedRequest(
                interceptedRequest = request,
                occurredAtEpochMilliseconds = clock.millis(),
            )

        val deferred = CompletableDeferred<InterceptDecision>()
        interceptQueue.register(request.messageId(), wrapped, deferred)
        emitCreatedEvent(wrapped)

        val decision =
            runBlocking {
                interceptQueue.awaitDecision(request.messageId(), awaitTimeoutSeconds)
                    ?: InterceptDecision.Forward()
            }

        interceptQueue.remove(request.messageId())
        emitResolvedEvent(wrapped.interceptIdentifier, decision)

        return when (decision) {
            is InterceptDecision.Forward -> {
                val targetRequest = decision.modifiedRequest ?: request
                // InterceptedRequest.annotations() 只在 InterceptedRequest 接口上；
                // 如果 targetRequest 是普通 HttpRequest（比如手机 sendToRepeater 来的），用默认空 annotations。
                val annotations =
                    if (targetRequest is InterceptedRequest) targetRequest.annotations() else Annotations.annotations()
                ProxyRequestReceivedAction.proxyRequestReceivedAction(
                    targetRequest,
                    annotations,
                    MessageReceivedAction.CONTINUE,
                )
            }
            is InterceptDecision.Drop -> ProxyRequestReceivedAction.drop()
        }
    }

    override fun handleRequestToBeSent(request: InterceptedRequest): ProxyRequestToBeSentAction =
        ProxyRequestToBeSentAction.continueWith(request)

    private fun emitCreatedEvent(wrapped: MontoyaInterceptedRequest) {
        // sequenceNumber 与 eventIdentifier 由 InMemoryRemoteEventStream.appendEvent 统一分配，
        // 这里传占位值即可——与 History/Repeater 发布者共用同一把递增源，避免撞号。
        eventStream.appendEvent(
            RemoteEventEnvelope(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                eventIdentifier = EventIdentifier(""),
                sequenceNumber = 0L,
                occurredAt = clock.instant(),
                eventType = INTERCEPT_CREATED_EVENT_TYPE,
                aggregateType = AggregateType.Intercept,
                aggregateIdentifier = AggregateIdentifier(wrapped.interceptIdentifier.value),
                payload = wrapped.buildMetadataPayload(),
            ),
        )
    }

    private fun emitResolvedEvent(
        interceptIdentifier: InterceptIdentifier,
        decision: InterceptDecision,
    ) {
        val eventType =
            when (decision) {
                is InterceptDecision.Forward -> INTERCEPT_FORWARDED_EVENT_TYPE
                is InterceptDecision.Drop -> INTERCEPT_DROPPED_EVENT_TYPE
            }
        val decisionText =
            when (decision) {
                is InterceptDecision.Forward -> DECISION_FORWARD
                is InterceptDecision.Drop -> DECISION_DROP
            }
        val payload =
            kotlinx.serialization.json.buildJsonObject {
                put(INTERCEPT_IDENTIFIER_FIELD, JsonPrimitive(interceptIdentifier.value))
                put(DECISION_FIELD, JsonPrimitive(decisionText))
            }
        eventStream.appendEvent(
            RemoteEventEnvelope(
                protocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
                eventIdentifier = EventIdentifier(""),
                sequenceNumber = 0L,
                occurredAt = clock.instant(),
                eventType = eventType,
                aggregateType = AggregateType.Intercept,
                aggregateIdentifier = AggregateIdentifier(interceptIdentifier.value),
                payload = payload,
            ),
        )
    }

    private companion object {
        private const val DEFAULT_AWAIT_TIMEOUT_SECONDS = 60L

        private const val INTERCEPT_CREATED_EVENT_TYPE = "intercept.created"

        private const val INTERCEPT_FORWARDED_EVENT_TYPE = "intercept.forwarded"

        private const val INTERCEPT_DROPPED_EVENT_TYPE = "intercept.dropped"

        private const val INTERCEPT_IDENTIFIER_FIELD = "interceptIdentifier"

        private const val DECISION_FIELD = "decision"

        private const val DECISION_FORWARD = "forward"

        private const val DECISION_DROP = "drop"
    }
}
