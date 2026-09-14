// 运行夹具：在 JVM 里起真实的远程控制服务端，供移动端跑端到端链路。

package xin.ctkqiang.burpsuite.remote.harness

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xin.ctkqiang.burpsuite.remote.adapter.BurpHistoryAdapter
import xin.ctkqiang.burpsuite.remote.adapter.BurpHistoryEventPublisher
import xin.ctkqiang.burpsuite.remote.adapter.BurpScopeAdapter
import xin.ctkqiang.burpsuite.remote.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.protocol.PairingTicketEncoder
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
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.exitProcess

/**
 * 启动真实的远程控制服务端并打印一张真实签发的配对票据；只在本机跑，不加载 Montoya，也不需要 Burp。
 */
fun main() {
    runBlocking { runHarness() }
    exitProcess(HARNESS_EXIT_CODE)
}

private suspend fun runHarness() {
    val clock = Clock.systemUTC()
    val advertisedHostAddress = advertisedHostAddress()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val pairedDeviceRegistry = PairedDeviceRegistry()
    val remotePort = resolveRemotePort()
    val devicePairingService =
        DevicePairingService(
            localNetworkAddressResolver = HarnessAdvertisedHostResolver(advertisedHostAddress),
            pairedDeviceRegistry = pairedDeviceRegistry,
            clock = clock,
            remotePort = remotePort,
        )

    val eventStream = InMemoryRemoteEventStream()
    val connectionRegistry = RemoteConnectionRegistry()

    // 事件用真实的发布者与真实的历史适配器产出：类型串与载荷字段全由插件自己写，夹具不编造任何字段名。
    val historySource = HarnessProxyHistorySource()
    val historyAdapter = BurpHistoryAdapter(historySource)
    val historySignalSource = HarnessProxyHistorySignalSource()

    // 作用域适配层复用同一个历史适配器：手机只发历史标识，主机串由插件读当下的事实拼出，两条链路因此不会对同一个标识给出两种地址。
    val scopeAdapter = BurpScopeAdapter(historyAdapter, HarnessScopeWriter())

    val remoteHttpServer =
        RemoteHttpServer(
            devicePairingService = devicePairingService,
            pairedDeviceRegistry = pairedDeviceRegistry,
            controlGate =
                RemoteControlGate(
                    pairedDeviceRegistry = pairedDeviceRegistry,
                    operationLog = InMemoryRemoteOperationLog(),
                    rateLimiter = RemoteDeviceRateLimiter(clock),
                    auditLogger = RemoteAuditLogger(::println),
                ),
            connectionRegistry = connectionRegistry,
            eventStream = eventStream,
            historyAdapter = historyAdapter,
            scopeAdapter = scopeAdapter,
            logSink = ::println,
            remotePort = remotePort,
        )

    val historyEventPublisher =
        BurpHistoryEventPublisher(
            historyAdapter = historyAdapter,
            historySignalSource = historySignalSource,
            eventStream = eventStream,
            sweepScope = scope,
        )

    println("== Burp Remote 服务端夹具 ==")
    println("监听端口：$remotePort；票据里的 host：$advertisedHostAddress")
    if (!remoteHttpServer.start()) {
        println("服务端启动失败，夹具退出。")
        exitProcess(HARNESS_EXIT_CODE)
    }
    historyEventPublisher.start()

    val ticketOutputPath = Path.of(ticketOutputPath())
    publishPairingTicket(devicePairingService = devicePairingService, ticketOutputPath = ticketOutputPath)
    scope.launch {
        while (true) {
            delay(ticketRefreshMilliseconds)
            publishPairingTicket(devicePairingService = devicePairingService, ticketOutputPath = ticketOutputPath)
        }
    }
    scope.launch {
        reportRemoteActivity(
            pairedDeviceRegistry = pairedDeviceRegistry,
            connectionRegistry = connectionRegistry,
            eventStream = eventStream,
        )
    }
    scope.launch {
        growProxyHistory(
            historySource = historySource,
            historySignalSource = historySignalSource,
            clock = clock,
            remotePort = remotePort,
        )
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            historyEventPublisher.stop()
            scope.cancel()
            remoteHttpServer.stop()
        },
    )
    awaitCancellation()
}

// 票据由真实服务签发、真实编码器编码；同时落一份到文件，运行脚本直接读它，不必去解析日志。
private fun publishPairingTicket(
    devicePairingService: DevicePairingService,
    ticketOutputPath: Path,
) {
    val ticket: PairingTicket = devicePairingService.openPairingSession()
    val encodedTicket = PairingTicketEncoder.encodeToText(ticket)
    Files.writeString(ticketOutputPath, encodedTicket)
    println(TICKET_JSON_PREFIX + encodedTicket)
    println(
        "票据要点：host=${ticket.host} port=${ticket.port} " +
            "protocolVersion=${ticket.protocolVersion} expiresAt=${ticket.expiresAt}",
    )
}

// 服务端自己不打印配对与认证日志，所以夹具盯着登记处与事件流的变化：这是核对链路走到哪一步的依据。
private suspend fun reportRemoteActivity(
    pairedDeviceRegistry: PairedDeviceRegistry,
    connectionRegistry: RemoteConnectionRegistry,
    eventStream: InMemoryRemoteEventStream,
) {
    var reportedPairedDeviceCount = 0
    var reportedConnectedDeviceCount = 0
    var reportedLatestSequenceNumber = eventStream.latestSequenceNumber()
    while (true) {
        delay(MONITOR_INTERVAL_MILLISECONDS)
        val pairedDevices = pairedDeviceRegistry.snapshot()
        if (pairedDevices.size > reportedPairedDeviceCount) {
            reportedPairedDeviceCount = pairedDevices.size
            println("配对：收到配对请求并已接受，登记设备=${pairedDevices.last().deviceIdentifier.value}")
        }
        val connectedDevices = connectionRegistry.snapshot()
        if (connectedDevices.size > reportedConnectedDeviceCount) {
            reportedConnectedDeviceCount = connectedDevices.size
            println("认证：事件通道已认证通过，设备=${connectedDevices.last().value}")
        }
        val latestSequenceNumber = eventStream.latestSequenceNumber()
        if (latestSequenceNumber > reportedLatestSequenceNumber) {
            reportedLatestSequenceNumber = latestSequenceNumber
            println("推送：事件流最新序号=$latestSequenceNumber")
        }
    }
}

// 定期让代理历史长一条：真实发布者照它产出事件，客户端因此有真实结构的事件可摄入。
private suspend fun growProxyHistory(
    historySource: HarnessProxyHistorySource,
    historySignalSource: HarnessProxyHistorySignalSource,
    clock: Clock,
    remotePort: Int,
) {
    val grownEntryCount = AtomicLong(0)
    while (true) {
        delay(EVENT_INTERVAL_MILLISECONDS)
        val entryOrdinal = grownEntryCount.incrementAndGet()
        historySource.appendEntry(
            HarnessProxyHistoryEntry(
                occurredAtEpochMilliseconds = clock.millis(),
                method = "GET",
                host = HARNESS_TARGET_HOST,
                port = HARNESS_TARGET_PORT,
                isSecure = true,
                destinationInternetProtocolAddress = HARNESS_TARGET_ADDRESS,
                path = "/harness/$entryOrdinal",
                listenerPort = remotePort,
                statusCode = 200,
                mimeTypeText = "JSON",
                responseLength = HARNESS_RESPONSE_LENGTH,
                durationMilliseconds = HARNESS_DURATION_MILLISECONDS,
            ),
        )
        historySignalSource.signalHistoryMayHaveGrown()
    }
}

// host 可被环境变量覆盖：模拟器访问宿主机要用 10.0.2.2，真实局域网里则让系统自己挑地址。
private fun advertisedHostAddress(): String {
    val overriddenHostAddress = System.getenv(HOST_ENVIRONMENT_VARIABLE)
    if (!overriddenHostAddress.isNullOrBlank()) {
        return overriddenHostAddress
    }
    return SystemLocalNetworkAddressResolver().resolveAdvertisedHostAddress() ?: LOOPBACK_HOST_ADDRESS
}

private fun ticketOutputPath(): String =
    System.getenv(TICKET_OUTPUT_PATH_ENVIRONMENT_VARIABLE) ?: DEFAULT_TICKET_OUTPUT_PATH

// 端口可被环境变量覆盖：本机若已有一个 Burp 实例占着默认端口，夹具换一个端口照样能跑完整的真实链路。
private fun resolveRemotePort(): Int = System.getenv(PORT_ENVIRONMENT_VARIABLE)?.toIntOrNull() ?: DEFAULT_REMOTE_PORT

private val ticketRefreshMilliseconds: Long =
    System.getenv(TICKET_REFRESH_SECONDS_ENVIRONMENT_VARIABLE)?.toLongOrNull()?.times(MILLISECONDS_PER_SECOND)
        ?: DEFAULT_TICKET_REFRESH_MILLISECONDS

private const val HOST_ENVIRONMENT_VARIABLE = "BURP_REMOTE_HARNESS_HOST"

private const val PORT_ENVIRONMENT_VARIABLE = "BURP_REMOTE_HARNESS_PORT"

private const val TICKET_OUTPUT_PATH_ENVIRONMENT_VARIABLE = "BURP_REMOTE_HARNESS_TICKET_PATH"

private const val TICKET_REFRESH_SECONDS_ENVIRONMENT_VARIABLE = "BURP_REMOTE_HARNESS_TICKET_REFRESH_SECONDS"

private const val DEFAULT_TICKET_OUTPUT_PATH = "/tmp/ticket.json"

// 默认四分钟重签一次：始终短于票据自身五分钟的有效期，文件里因此永远躺着一张还能用的票。
private const val DEFAULT_TICKET_REFRESH_MILLISECONDS = 240_000L

private const val MILLISECONDS_PER_SECOND = 1_000L

private const val MONITOR_INTERVAL_MILLISECONDS = 500L

private const val EVENT_INTERVAL_MILLISECONDS = 3_000L

private const val HARNESS_TARGET_HOST = "harness.example.com"

private const val HARNESS_TARGET_PORT = 443

private const val HARNESS_TARGET_ADDRESS = "203.0.113.10"

private const val HARNESS_RESPONSE_LENGTH = 512L

private const val HARNESS_DURATION_MILLISECONDS = 37L

private const val LOOPBACK_HOST_ADDRESS = "127.0.0.1"

private const val TICKET_JSON_PREFIX = "TICKET_JSON="

private const val HARNESS_EXIT_CODE = 0
