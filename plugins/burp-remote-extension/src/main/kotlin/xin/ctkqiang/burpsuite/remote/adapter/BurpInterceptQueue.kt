// 挂起式拦截队列：Montoya handler 在这里阻塞等待手机命令，REST endpoint 在这里解挂。

package xin.ctkqiang.burpsuite.remote.adapter

import burp.api.montoya.http.message.requests.HttpRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import xin.ctkqiang.burpsuite.remote.protocol.InterceptIdentifier
import kotlin.time.Duration.Companion.seconds

/**
 * 手机能发给插件的拦截决策。
 *
 * [Forward] 里带可选的 [modifiedRequest]：手机不改就传 null（放行原始请求），
 * 改了就把完整请求塞进来（放行修改后的版本）。这样一个枚举值就覆盖了 Forward 和 Modify 两条命令。
 */
sealed class InterceptDecision {
    /** 放行；[modifiedRequest] 为 null 时放行原始请求，非 null 时放行修改后的版本。 */
    data class Forward(val modifiedRequest: HttpRequest? = null) : InterceptDecision()

    /** 丢弃；不发任何报文。 */
    data object Drop : InterceptDecision()
}

/**
 * 线程安全的拦截队列。
 *
 * Montoya 的 ProxyRequestHandler 在 Burp 代理线程上回调，handler 里用 [runBlocking] + [awaitDecision] 阻塞等待。
 * REST endpoint 在 Ktor 的协程线程上收到手机命令，调用 [submitDecision] 解挂。
 * 两边用同一把锁保证 CompletableDeferred 的创建和 complete 不会竞态。
 */
class BurpInterceptQueue {
    private val lock = Any()

    // messageId → 等待手机决策的挂起点。
    private val pendingDecisions = LinkedHashMap<Int, CompletableDeferred<InterceptDecision>>()

    // messageId → 被拦截消息的包装；REST 端点读元数据/报文详情用。
    private val pendingRequests = LinkedHashMap<Int, MontoyaInterceptedRequest>()

    /**
     * Montoya handler 回调进来时调用：登记挂起点和被拦截消息元数据。
     *
     * @return 用于 runBlocking 等待的 CompletableDeferred；**不会**为 null。
     */
    fun register(
        messageId: Int,
        intercepted: MontoyaInterceptedRequest,
        deferred: CompletableDeferred<InterceptDecision>,
    ) {
        synchronized(lock) {
            pendingDecisions[messageId] = deferred
            pendingRequests[messageId] = intercepted
        }
    }

    /** Montoya handler 在 runBlocking 里调用：挂起直到手机命令到达或超时。 */
    suspend fun awaitDecision(
        messageId: Int,
        timeout: Long,
    ): InterceptDecision? {
        val deferred =
            synchronized(lock) {
                pendingDecisions[messageId] ?: return null
            }
        return withTimeoutOrNull(timeout.seconds) { deferred.await() }
    }

    /** REST endpoint 收到手机命令时调用：解挂对应挂起点。 */
    fun submitDecision(
        interceptIdentifier: InterceptIdentifier,
        decision: InterceptDecision,
    ): Boolean {
        val messageId = messageIdFromIdentifier(interceptIdentifier) ?: return false
        val deferred =
            synchronized(lock) {
                pendingDecisions.remove(messageId)
            }
        deferred?.complete(decision)
        return deferred != null
    }

    /** REST `GET /v1/intercepts` 读队列快照。 */
    fun snapshot(): List<MontoyaInterceptedRequest> =
        synchronized(lock) {
            pendingRequests.values.toList()
        }

    /** REST `GET /v1/intercepts/{id}` 读单条。 */
    fun find(interceptIdentifier: InterceptIdentifier): MontoyaInterceptedRequest? {
        val messageId = messageIdFromIdentifier(interceptIdentifier) ?: return null
        return synchronized(lock) { pendingRequests[messageId] }
    }

    /** handler 拿到决策（或超时）后调用：从队列里移除。 */
    fun remove(messageId: Int) {
        synchronized(lock) {
            pendingDecisions.remove(messageId)
            pendingRequests.remove(messageId)
        }
    }

    /** 插件卸载时一次性清空：所有挂起点默认 Forward，避免卸载后协程继续阻塞。 */
    fun clear() {
        synchronized(lock) {
            pendingDecisions.values.forEach { it.complete(InterceptDecision.Forward()) }
            pendingDecisions.clear()
            pendingRequests.clear()
        }
    }

    private fun messageIdFromIdentifier(interceptIdentifier: InterceptIdentifier): Int? {
        val raw = interceptIdentifier.value
        if (!raw.startsWith(INTERCEPT_IDENTIFIER_PREFIX)) return null
        return raw.substringAfter(INTERCEPT_IDENTIFIER_PREFIX).toIntOrNull()
    }

    private companion object {
        private const val INTERCEPT_IDENTIFIER_PREFIX = "intercept_"
    }
}
