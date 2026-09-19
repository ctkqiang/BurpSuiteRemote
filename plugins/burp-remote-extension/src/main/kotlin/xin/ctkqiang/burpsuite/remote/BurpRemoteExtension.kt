// 扩展入口：唯一的 BurpExtension 实现，只负责接线。

package xin.ctkqiang.burpsuite.remote

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Registration
import burp.api.montoya.http.message.requests.HttpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import xin.ctkqiang.burpsuite.remote.adapter.BurpHistoryAdapter
import xin.ctkqiang.burpsuite.remote.adapter.BurpHistoryEventPublisher
import xin.ctkqiang.burpsuite.remote.adapter.BurpInterceptProxyRequestHandler
import xin.ctkqiang.burpsuite.remote.adapter.BurpInterceptQueue
import xin.ctkqiang.burpsuite.remote.adapter.BurpRepeaterStore
import xin.ctkqiang.burpsuite.remote.adapter.BurpScopeAdapter
import xin.ctkqiang.burpsuite.remote.adapter.MontoyaProxyHistorySignalSource
import xin.ctkqiang.burpsuite.remote.adapter.MontoyaProxyHistorySource
import xin.ctkqiang.burpsuite.remote.adapter.MontoyaScopeWriter
import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.security.DevicePairingService
import xin.ctkqiang.burpsuite.remote.security.PairedDeviceRegistry
import xin.ctkqiang.burpsuite.remote.transport.InMemoryRemoteEventStream
import xin.ctkqiang.burpsuite.remote.transport.InMemoryRemoteOperationLog
import xin.ctkqiang.burpsuite.remote.transport.RemoteAuditLogger
import xin.ctkqiang.burpsuite.remote.transport.RemoteConnectionRegistry
import xin.ctkqiang.burpsuite.remote.transport.RemoteControlGate
import xin.ctkqiang.burpsuite.remote.transport.RemoteDeviceRateLimiter
import xin.ctkqiang.burpsuite.remote.transport.RemoteHttpServer
import xin.ctkqiang.burpsuite.remote.transport.SystemLocalNetworkAddressResolver
import xin.ctkqiang.burpsuite.remote.userinterface.RemoteStatusPanel
import java.time.Clock
import java.util.Locale

/**
 * Burp Remote 的扩展入口。
 *
 * Montoya 会自行实例化本类并调用 [initialize]，所以要保留公开无参构造器；构造器里不做副作用，初始化失败时 Burp 才能把异常归到本扩展。
 */
class BurpRemoteExtension : BurpExtension {
    /**
     * 扩展初始化钩子，由 Burp 在加载完成后调用一次。
     *
     * 装配远程端点、装配配对与安全闸门、注册标签页并登记卸载回调；卸载回调只能在仍是已加载状态时注册，所以注册句柄要一直存活。
     */
    override fun initialize(montoyaApi: MontoyaApi) {
        montoyaApi.extension().setName(EXTENSION_NAME)
        // 日志只记状态，不记配对码：Burp 输出面板常被整段复制进缺陷报告，凭证出现在那里就等于公开。
        montoyaApi.logging().logToOutput("Burp Remote 已加载：$EXTENSION_NAME")

        // 时钟只建一次并注入各方：有效期由安全层判定、倒计时由界面展示、限流按它计算，三处必须读同一个时间源。
        val clock = Clock.systemUTC()

        // 登记处以同一个实例同时交给服务与界面：各持一份，界面就会显示与真实授权状态无关的列表。
        val pairedDeviceRegistry = PairedDeviceRegistry()

        val devicePairingService =
            DevicePairingService(
                localNetworkAddressResolver = SystemLocalNetworkAddressResolver(),
                pairedDeviceRegistry = pairedDeviceRegistry,
                clock = clock,
                remotePort = DEFAULT_REMOTE_PORT,
            )

        // 事件日志只建一次并交给发布者与传输层：各持一份的话，事件会写进没有人读的那一份。
        val eventStream = InMemoryRemoteEventStream()

        val historyAdapter = BurpHistoryAdapter(MontoyaProxyHistorySource(montoyaApi.proxy()))

        // 作用域适配层复用同一个历史适配器：手机只发历史标识，主机串由插件读 Burp 当下的事实拼出。
        val scopeAdapter = BurpScopeAdapter(historyAdapter, MontoyaScopeWriter(montoyaApi.scope()))

        // 兜底扫描需要自己的作用域：生命周期与扩展绑定，卸载时连同订阅一起取消。
        val historySweepScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val historyEventPublisher =
            BurpHistoryEventPublisher(
                historyAdapter = historyAdapter,
                historySignalSource = MontoyaProxyHistorySignalSource(montoyaApi.http()),
                eventStream = eventStream,
                sweepScope = historySweepScope,
            )

        // 拦截队列在 RemoteHttpServer 之前建好：handler 和 REST 端点要共享它。
        val interceptQueue = BurpInterceptQueue()

        // Remote Repeater 内存存储：手机推过来的请求存在这里，execute 时从这里取。
        val repeaterStore = BurpRepeaterStore()

        // 拦截 handler 的"是否有活跃设备"判定：已配对设备 ∩ 已连接设备，两边至少有一个交集。
        val connectionRegistry = RemoteConnectionRegistry()
        val hasActivePairedDevice = {
            val connected = connectionRegistry.snapshot().toSet()
            val paired = pairedDeviceRegistry.snapshot().map { it.deviceIdentifier }.toSet()
            connected.intersect(paired).isNotEmpty()
        }

        val interceptHandler =
            BurpInterceptProxyRequestHandler(
                interceptQueue = interceptQueue,
                eventStream = eventStream,
                hasActivePairedDevice = hasActivePairedDevice,
                clock = clock,
            )

        // 注册 handler 到 Montoya Proxy；返回的 Registration 要在卸载时 deregister。
        val interceptHandlerRegistration = montoyaApi.proxy().registerRequestHandler(interceptHandler)

        val remoteHttpServer =
            RemoteHttpServer(
                devicePairingService = devicePairingService,
                pairedDeviceRegistry = pairedDeviceRegistry,
                controlGate = createControlGate(pairedDeviceRegistry, clock, montoyaApi),
                connectionRegistry = connectionRegistry,
                eventStream = eventStream,
                historyAdapter = historyAdapter,
                scopeAdapter = scopeAdapter,
                logSink = montoyaApi.logging()::logToOutput,
                clock = clock,
                interceptQueue = interceptQueue,
                repeaterStore = repeaterStore,
                montoyaHttp = montoyaApi.http(),
                sendToRepeater = { request, tabName -> sendToRepeater(montoyaApi, request, tabName) },
            )

        // 先启动端点再建界面：界面要显示的是真实状态，晚一步启动会让它显示一瞬「未启动」。
        remoteHttpServer.start()

        // 历史事件必须在端点之后订阅，但要在客户端连上来之前就绪，否则最早几条记录会从事件流里漏掉。
        historyEventPublisher.start()

        val statusPanel =
            createStatusPanel(
                clock = clock,
                devicePairingService = devicePairingService,
                pairedDeviceRegistry = pairedDeviceRegistry,
                remoteHttpServer = remoteHttpServer,
            )

        // 标签页继承 Burp 当前主题，否则在深色模式下会是一块刺眼的浅色区域。
        montoyaApi.userInterface().applyThemeToComponent(statusPanel)

        // 只加载 JAR 不会产生任何界面，标签页必须在此显式注册。
        val suiteTabRegistration = montoyaApi.userInterface().registerSuiteTab(EXTENSION_NAME, statusPanel)

        montoyaApi.extension().registerUnloadingHandler {
            unloadResources(
                historyEventPublisher = historyEventPublisher,
                historySweepScope = historySweepScope,
                interceptQueue = interceptQueue,
                repeaterStore = repeaterStore,
                interceptHandlerRegistration = interceptHandlerRegistration,
                remoteHttpServer = remoteHttpServer,
                suiteTabRegistration = suiteTabRegistration,
            )
            montoyaApi.logging().logToOutput("Burp Remote 正在卸载：$EXTENSION_NAME")
        }
    }

    // 审计与限流只在这里装配：安全闸门要的是一个能写日志、能计时的实例，而不是让传输层自己去取 Burp API。
    private fun createControlGate(
        pairedDeviceRegistry: PairedDeviceRegistry,
        clock: Clock,
        montoyaApi: MontoyaApi,
    ): RemoteControlGate =
        RemoteControlGate(
            pairedDeviceRegistry = pairedDeviceRegistry,
            operationLog = InMemoryRemoteOperationLog(),
            rateLimiter = RemoteDeviceRateLimiter(clock),
            auditLogger = RemoteAuditLogger(montoyaApi.logging()::logToOutput),
        )

    // 状态面板装配独立成方法：语言、服务状态、配对票据这些输入都由 initialize 显式传入。
    private fun createStatusPanel(
        clock: Clock,
        devicePairingService: DevicePairingService,
        pairedDeviceRegistry: PairedDeviceRegistry,
        remoteHttpServer: RemoteHttpServer,
    ): RemoteStatusPanel =
        RemoteStatusPanel(
            extensionName = EXTENSION_NAME,
            clock = clock,
            // 语言在这里解析一次再注入，控件内部不读环境，于是它成了可替换的输入而不是隐式依赖。
            locale = Locale.getDefault(),
            // 界面每次刷新都现读，服务真被端口占用而没起来时，界面不会替它说谎。
            isRemoteServerRunning = remoteHttpServer::isRunning,
            pairingTicketSupplier = devicePairingService::openPairingSession,
            pairedDeviceSupplier = pairedDeviceRegistry::snapshot,
            // 用 lambda 而不是方法引用：移除方法返回是否命中，而面板只关心请求已发出、随后重读列表。
            pairedDeviceRevoker = { deviceIdentifier ->
                pairedDeviceRegistry.removePairedDevice(deviceIdentifier)
            },
        )

    // 卸载清理单独成方法：拆卸顺序与装配顺序相反，注释跟着逻辑走而不是堆在 initialize 里。
    private fun unloadResources(
        historyEventPublisher: BurpHistoryEventPublisher,
        historySweepScope: CoroutineScope,
        interceptQueue: BurpInterceptQueue,
        repeaterStore: BurpRepeaterStore,
        interceptHandlerRegistration: Registration,
        remoteHttpServer: RemoteHttpServer,
        suiteTabRegistration: Registration,
    ) {
        // 先停历史事件：卸载后任何残留回调再去读代理历史，就会在已卸下扩展的 Burp 上抛错。
        historyEventPublisher.stop()
        historySweepScope.cancel()
        // 先清拦截队列：让所有挂起的 handler 解挂（默认 Forward），再 deregister handler，
        // 避免 handler 还在执行时 queue 就被回收的竞态。
        interceptQueue.clear()
        repeaterStore.clear()
        interceptHandlerRegistration.deregister()
        // 先停服务器：端口与线程不清干净时，重新加载扩展会因为端口仍被自己占用而启动失败。
        remoteHttpServer.stop()
        // 卸载时主动摘掉标签页：Burp 不替扩展回收组件，留着会残留界面，重载时还会叠出两个同名标签页。
        suiteTabRegistration.deregister()
    }

    // Burp Repeater 的 sendToRepeater：tabName 传 null 或具体标签名。
    private fun sendToRepeater(
        montoyaApi: MontoyaApi,
        request: HttpRequest,
        tabName: String?,
    ) {
        if (tabName != null) {
            montoyaApi.repeater().sendToRepeater(request, tabName)
        } else {
            montoyaApi.repeater().sendToRepeater(request)
        }
    }

    private companion object {
        // 产品名不参与本地化，Burp 的扩展列表只有英文，改名会和支持工单里记的扩展名对不上。
        private const val EXTENSION_NAME = "Burp Remote"
    }
}
