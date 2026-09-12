package xin.ctkqiang.burpsuite.remote.mobileapp

import android.content.Context
import androidx.room.Room
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.AndroidTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.BurpRemoteDatabase
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventIngestor
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.JournalEventRecordedEventMapper
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.RoomEventJournal
import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.RoomProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.ProjectionRebuilder
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.ProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.RoomHistoryProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.RoomInterceptProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.remote.KtorRemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.data.remote.PersistingRemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.data.remote.RemoteHttpClientFactory
import xin.ctkqiang.burpsuite.remote.mobileapp.data.remote.RemoteRestClient
import xin.ctkqiang.burpsuite.remote.mobileapp.data.remote.RemoteResynchronisation
import xin.ctkqiang.burpsuite.remote.mobileapp.data.remote.StoredDeviceIdentifierProvider
import xin.ctkqiang.burpsuite.remote.mobileapp.data.repository.ProjectionDashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.data.repository.RoomHistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.data.repository.RoomInterceptRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.AndroidKeystoreSecretCipher
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.DataStoreRemoteConnectionSettingsStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.DataStoreSettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.RemoteConnectionSettingsStore
import xin.ctkqiang.burpsuite.remote.mobileapp.data.transaction.RoomAtomicUnitOfWork
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.EventJournal
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.journal.ProjectionCheckpointStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteTimeouts
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.sync.EventSynchronisationCoordinator
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.NavigationDependencies
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingCoordinator
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 依赖装配处。全应用只有这里知道接口背后的实现是谁，也只有这里碰得到 Room 与 HTTP 客户端。
 *
 * 事件摄入与连接都挂在进程级作用域上：它们必须活过任何一次界面重建，转屏掉线是这一类错误里最贵的一种。
 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    /** 技术日志；装配层与导航壳都往这里写，界面自身不碰它。 */
    val technicalLog: TechnicalLog = AndroidTechnicalLog()

    private val processScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val timeProvider = TimeProvider { Instant.now() }

    // 库里放的是事件日志与投影，属于敏感数据，留在应用私有目录（plan §58）。
    private val database: BurpRemoteDatabase =
        Room.databaseBuilder(applicationContext, BurpRemoteDatabase::class.java, DATABASE_FILE_NAME).build()

    private val connectionSettingsStore: RemoteConnectionSettingsStore =
        DataStoreRemoteConnectionSettingsStore(applicationContext, AndroidKeystoreSecretCipher())

    /**
     * 设置的读写端口：外观，以及插件地址端口与配对状态。
     *
     * 地址端口与设备身份同住连接配置那一个文件，因此这里把连接端口交给它，而不是再存一份——
     * 两份地址迟早会不一致，那时用户改的那一份可能不是生效的那一份。
     */
    val settingsRepository: SettingsRepository =
        DataStoreSettingsRepository(applicationContext, connectionSettingsStore)

    private val deviceIdentifierProvider = StoredDeviceIdentifierProvider(connectionSettingsStore)

    private val eventJournal: EventJournal = RoomEventJournal(database.remoteEventJournalTable(), timeProvider)

    private val projectionCheckpointStore: ProjectionCheckpointStore =
        RoomProjectionCheckpointStore(database.projectionCheckpointTable())

    private val projectionStores: List<ProjectionStore> =
        listOf(
            RoomHistoryProjectionStore(database.historyRecordTable()),
            RoomInterceptProjectionStore(database.interceptRecordTable()),
        )

    private val atomicUnitOfWork = RoomAtomicUnitOfWork(database)

    private val recordedEventMapper = JournalEventRecordedEventMapper

    private val synchronisationCoordinator = EventSynchronisationCoordinator()

    private val journalEventIngestor =
        JournalEventIngestor(
            eventJournal = eventJournal,
            projectionCheckpointStore = projectionCheckpointStore,
            projectionStores = projectionStores,
            synchronisationCoordinator = synchronisationCoordinator,
            recordedEventMapper = recordedEventMapper,
            atomicUnitOfWork = atomicUnitOfWork,
            timeProvider = timeProvider,
            technicalLog = technicalLog,
        )

    private val projectionRebuilder =
        ProjectionRebuilder(
            eventJournal = eventJournal,
            projectionCheckpointStore = projectionCheckpointStore,
            recordedEventMapper = recordedEventMapper,
            atomicUnitOfWork = atomicUnitOfWork,
            timeProvider = timeProvider,
        )

    private val httpClient: HttpClient = RemoteHttpClientFactory.create()

    private val restClient =
        RemoteRestClient(
            httpClient = httpClient,
            deviceIdentifierProvider = deviceIdentifierProvider,
            timeouts = REMOTE_TIMEOUTS,
            technicalLog = technicalLog,
        )

    private val remoteResynchronisation =
        RemoteResynchronisation(
            snapshotSource = restClient,
            resynchronisationRecorder = synchronisationCoordinator,
            projectionRebuilder = projectionRebuilder,
            projectionStores = projectionStores,
            technicalLog = technicalLog,
        )

    private val remoteTransport =
        KtorRemoteControlClient(
            restClient = restClient,
            deviceIdentifierProvider = deviceIdentifierProvider,
            journalEventIngestor = journalEventIngestor,
            resumptionSequenceNumberProvider = synchronisationCoordinator,
            resynchronisation = remoteResynchronisation,
            technicalLog = technicalLog,
        )

    /**
     * 远程控制端口。
     *
     * 外面这层负责两件传输层不该管的事：配对成功后落地身份与地址，以及把连接挂到进程级作用域。
     */
    val remoteControlClient: RemoteControlClient =
        PersistingRemoteControlClient(
            delegate = remoteTransport,
            connectionSettingsStore = connectionSettingsStore,
            connectionScope = processScope,
            technicalLog = technicalLog,
        )

    /** 连接状态；界面与主面板都从这里读，谁都不去问传输层的内部状态。 */
    val connectionState: Flow<ConnectionState> = remoteControlClient.connectionState

    /** 历史记录的读端口，读的是投影而不是事实源。 */
    val historyRepository: HistoryRepository = RoomHistoryRepository(database.historyRecordTable())

    /** 拦截项的读端口。 */
    val interceptRepository: InterceptRepository = RoomInterceptRepository(database.interceptRecordTable())

    /** 主面板汇总：由历史投影、拦截投影与连接状态三路合并而来。 */
    val dashboardRepository: DashboardRepository =
        ProjectionDashboardRepository(
            historyRepository = historyRepository,
            interceptRepository = interceptRepository,
            connectionState = connectionState,
        )

    /** 配对入口：扫码文本与手输文本都走它。 */
    val remotePairingCoordinator: RemotePairingCoordinator =
        EncodedTicketPairingCoordinator(
            remoteControlClient = remoteControlClient,
            timeProvider = timeProvider,
            technicalLog = technicalLog,
        )

    private val hasStarted = AtomicBoolean(false)

    /** 启动后台链路：按本地日志校准续传基准，若已配对则直接建连。进程启动时调用一次。 */
    fun start() {
        if (!hasStarted.compareAndSet(false, true)) return
        processScope.launch { restoreSynchronisationBaselineAndConnect() }
    }

    /** 干净停止：先收掉连接与事件摄入所在的作用域，再放掉传输栈与库。 */
    fun stop() {
        processScope.cancel()
        httpClient.close()
        database.close()
    }

    /** 交给导航壳的依赖表；导航壳只认这一个入参。 */
    fun navigationDependencies(): NavigationDependencies =
        NavigationDependencies(
            dashboardRepository = dashboardRepository,
            historyRepository = historyRepository,
            interceptRepository = interceptRepository,
            settingsRepository = settingsRepository,
            remoteControlClient = remoteControlClient,
            remotePairingCoordinator = remotePairingCoordinator,
            // 日志端口由壳分给各屏，各屏不再各自去拿全局日志。
            technicalLog = technicalLog,
        )

    // 续传基准必须以本地日志的最新序号为初值：用 0 起步会把「已经收到过」判成断洞，
    // 于是每次重启都会先卡在补齐状态（plan §9）。
    private suspend fun restoreSynchronisationBaselineAndConnect() {
        val latestSequenceNumber = eventJournal.latestSequenceNumber()
        synchronisationCoordinator.markResynchronised(latestSequenceNumber)
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.EventIngestion,
                message = "续传基准已按本地事件日志校准",
                attributes = mapOf("latestSequenceNumber" to latestSequenceNumber.toString()),
            ),
        )

        if (connectionSettingsStore.readDeviceIdentifier() == null) {
            technicalLog.record(
                TechnicalLogEvent(
                    category = TechnicalLogCategory.Pairing,
                    message = "尚未配对，保持离线；界面照实显示 OFFLINE",
                ),
            )
            return
        }

        val savedConfiguration = connectionSettingsStore.readConnectionConfiguration()
        if (savedConfiguration == null) {
            technicalLog.record(
                TechnicalLogEvent(
                    category = TechnicalLogCategory.Pairing,
                    message = "没有可用的连接配置，跳过自动连接",
                ),
            )
            return
        }
        remoteControlClient.connect(savedConfiguration)
    }

    private companion object {
        const val DATABASE_FILE_NAME = "burp-remote.db"

        val REMOTE_TIMEOUTS = RemoteTimeouts()
    }
}
