package xin.ctkqiang.burpsuite.remote.mobileapp

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.RemoteServerEndpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection.BurpConnectionRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard.DashboardRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.history.HistoryDetailRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.history.HistoryRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept.InterceptDetailRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept.InterceptRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater.RepeaterRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguageRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.SecurityScreen
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.SettingsRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.StorageScreen
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing.SharingRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LiveOfflineBadge
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteScaffold
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteScanIcon
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTopBar
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTopBarActionButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.BurpRemoteNavigationBar
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.BurpRemoteRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.NavigationDependencies
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingConclusion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.SectionMenuScreen
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.TopLevelSection
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R as UserInterfaceResources

/**
 * 应用导航壳：设计系统外壳加各屏装配（plan §43）。
 *
 * NavHost 只能建在这一层：feature 模块不认识 NavController，只接受回调（rules.md §6.1）。
 * 壳只认 [NavigationDependencies]，因此「哪一屏能做到什么」在编译期就定死了。
 *
 * 顶栏与底栏也归这一层，且各自只有一条规则：
 * - 顶栏全应用唯一，标题取当前目的地；只有子页面有返回箭头；动作集合按目的地固定，不随页面拼装而变。
 * - 底栏只在四个一级目的地出现；进入子页面就收起，因为一级之间才是平级切换。
 *
 * 模态流程（扫码）不占路由：它画在外壳之上，扫到的文本与手输的文本走同一个配对入口。
 *
 * @param startRoute 额外的落地路由；只在调试入口使用，取值不在白名单时回落到主页。
 * @param pairingTicketText 由调试入口注入的票据文本；走与扫码完全相同的配对入口，因此它证明的就是扫码这条路。
 */
@Composable
fun BurpsuiteRemoteNavigationHost(
    navigationDependencies: NavigationDependencies,
    startRoute: String? = null,
    pairingTicketText: String? = null,
) {
    val technicalLog = navigationDependencies.technicalLog
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedSection =
        TopLevelSection.entries.firstOrNull { section -> section.containsRoute(currentRoute) }
    val isPrimaryDestination = currentRoute in PRIMARY_DESTINATIONS

    // 连接状态在界面上只有一种来源；各屏读的是同一份，不各自去问传输层。
    val connectionState: Flow<ConnectionState> =
        remember(navigationDependencies) { connectionStateOf(navigationDependencies) }
    val currentConnectionState: ConnectionState by connectionState.collectAsState(
        initial = ConnectionState.Disconnected,
    )

    // 顶栏副标题写「连的是哪台机器」：连接屏上这一行比标题更有用。
    val savedEndpoint: RemoteServerEndpoint? by
        remember(navigationDependencies) {
            navigationDependencies.settingsRepository.observeServerEndpoint()
        }.collectAsState(initial = null)

    // 扫码弹窗的全部状态；它不占路由，所以由壳自己持有。
    var isScannerOpen by remember { mutableStateOf(false) }
    var isPairingInFlight by remember { mutableStateOf(false) }
    var pairingConclusion by remember { mutableStateOf<RemotePairingConclusion?>(null) }
    var submittedTicketText by remember { mutableStateOf<String?>(null) }
    val pairingScope = rememberCoroutineScope()

    val submitTicket: (String) -> Unit = { ticketText ->
        // 同一张票据只提交一次：识别是逐帧的，一张过期票据一直举在镜头前会被反复提交。
        if (ticketText != submittedTicketText && !isPairingInFlight) {
            submittedTicketText = ticketText
            isPairingInFlight = true
            pairingConclusion = null
            technicalLog.record(
                TechnicalLogEvent(
                    category = TechnicalLogCategory.Pairing,
                    message = "拿到票据文本，交给配对链路",
                    // 只记文本长度：票据原文里带着一次性配对码，属于凭据（rules.md §12）。
                    attributes = mapOf("ticketTextLength" to ticketText.length.toString()),
                ),
            )
            pairingScope.launch {
                val conclusion =
                    navigationDependencies.remotePairingCoordinator
                        ?.pairWithEncodedTicket(ticketText)
                        ?: RemotePairingConclusion.Failed
                isPairingInFlight = false
                pairingConclusion = conclusion
                technicalLog.record(
                    TechnicalLogEvent(
                        category =
                            if (conclusion == RemotePairingConclusion.Succeeded) {
                                TechnicalLogCategory.Pairing
                            } else {
                                TechnicalLogCategory.Failure
                            },
                        message = "配对结论",
                        attributes = mapOf("conclusion" to conclusion.name),
                    ),
                )
                if (conclusion == RemotePairingConclusion.Succeeded) {
                    // 配对成功就把弹窗收掉并回到主面板：用户要看的是 LIVE，不是仍旧举着的相机。
                    isScannerOpen = false
                    navController.openRoute(BurpRemoteRoute.DASHBOARD)
                }
            }
        }
    }

    // 路由切换是排查「点了没反应」的第一手材料，因此每一次变化都记一条。
    LaunchedEffect(currentRoute) {
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.Navigation,
                message = "路由切换",
                attributes = mapOf("route" to currentRoute.orEmpty()),
            ),
        )
    }

    // 调试入口注入的票据与扫码合流：这里没有第二条配对路径。
    LaunchedEffect(pairingTicketText) {
        if (!pairingTicketText.isNullOrBlank()) submitTicket(pairingTicketText)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BurpRemoteScaffold(
            topBar = {
                BurpRemoteTopBar(
                    title = stringResource(titleResourceOf(currentRoute)),
                    subtitle =
                        if (currentRoute in CONNECTION_ROUTES) {
                            endpointTextOf(endpoint = savedEndpoint)
                        } else {
                            null
                        },
                    onNavigateBack =
                        if (isPrimaryDestination || currentRoute == null) {
                            null
                        } else {
                            { navController.navigateUp() }
                        },
                    showsBrandMark = isPrimaryDestination,
                    actions = {
                        // 连接状态是常驻事实，任何页面都在同一位置说同一件事；扫码只在主面板是动作。
                        LiveOfflineBadge(connectionState = currentConnectionState)
                        if (currentRoute == BurpRemoteRoute.DASHBOARD) {
                            // 动作图标自带 48dp 触控区，这里只留最小的视觉间隔，否则图标看起来离状态胶囊很远。
                            Spacer(modifier = Modifier.width(BurpRemoteSpacing.ExtraSmall))
                            BurpRemoteTopBarActionButton(
                                icon = BurpRemoteScanIcon,
                                contentDescription = stringResource(R.string.navigation_action_scan_pairing),
                                onClick = {
                                    technicalLog.record(
                                        TechnicalLogEvent(
                                            category = TechnicalLogCategory.Pairing,
                                            message = "打开扫码弹窗",
                                        ),
                                    )
                                    // 每开一次就允许重新提交：上一张票据失败后，用户要能再扫一次。
                                    submittedTicketText = null
                                    pairingConclusion = null
                                    isScannerOpen = true
                                },
                            )
                        }
                    },
                )
            },
            bottomBar = {
                // 底栏只属于一级目的地：子页面与模态流程里它只会挡住返回路径。
                if (isPrimaryDestination) {
                    BurpRemoteNavigationBar(
                        selectedSection = selectedSection,
                        onSectionSelected = { section ->
                            technicalLog.record(
                                TechnicalLogEvent(
                                    category = TechnicalLogCategory.Navigation,
                                    message = "切换一级分区",
                                    attributes = mapOf("section" to section.name),
                                ),
                            )
                            navController.openRoute(section.landingRoute)
                        },
                    )
                }
            },
        ) { contentPadding ->
            NavHost(
                navController = navController,
                startDestination = resolveStartRoute(requestedRoute = startRoute),
                modifier = Modifier.padding(contentPadding),
            ) {
                composable(BurpRemoteRoute.DASHBOARD) {
                    DashboardRoute(
                        dashboardRepository = navigationDependencies.dashboardRepository,
                        historyRepository = navigationDependencies.historyRepository,
                        onOpenHistoryRecord = { historyIdentifier ->
                            navController.openRoute(BurpRemoteRoute.liveHistoryDetail(historyIdentifier))
                        },
                        technicalLog = technicalLog,
                    )
                }

                composable(BurpRemoteRoute.LIVE_SECTION) {
                    SectionMenuScreen(
                        entries = TopLevelSection.Live.indexEntries,
                        onOpenRoute = navController::openRoute,
                    )
                }
                composable(BurpRemoteRoute.LIVE_HISTORY) {
                    HistoryRoute(
                        historyRepository = navigationDependencies.historyRepository,
                        onOpenHistoryRecord = { historyIdentifier ->
                            navController.openRoute(BurpRemoteRoute.liveHistoryDetail(historyIdentifier))
                        },
                        technicalLog = technicalLog,
                    )
                }
                composable(
                    route = BurpRemoteRoute.LIVE_HISTORY_DETAIL,
                    arguments =
                        listOf(
                            navArgument(BurpRemoteRoute.HISTORY_IDENTIFIER_ARGUMENT) { type = NavType.StringType },
                        ),
                ) { detailEntry ->
                    val historyIdentifier =
                        detailEntry.arguments?.getString(BurpRemoteRoute.HISTORY_IDENTIFIER_ARGUMENT)
                    if (historyIdentifier != null) {
                        HistoryDetailRoute(
                            historyIdentifier = historyIdentifier,
                            historyRepository = navigationDependencies.historyRepository,
                            onOpenSharing = { identifier ->
                                navController.openRoute(BurpRemoteRoute.sharing(identifier))
                            },
                            remoteHistoryMessageReader = navigationDependencies.remoteHistoryMessageReader,
                            technicalLog = technicalLog,
                        )
                    }
                }
                composable(BurpRemoteRoute.LIVE_INTERCEPT) {
                    InterceptRoute(
                        interceptRepository = navigationDependencies.interceptRepository,
                        onOpenInterceptRecord = { interceptIdentifier ->
                            navController.openRoute(BurpRemoteRoute.liveInterceptDetail(interceptIdentifier))
                        },
                        technicalLog = technicalLog,
                    )
                }
                composable(
                    route = BurpRemoteRoute.LIVE_INTERCEPT_DETAIL,
                    arguments =
                        listOf(
                            navArgument(BurpRemoteRoute.INTERCEPT_IDENTIFIER_ARGUMENT) { type = NavType.StringType },
                        ),
                ) { detailEntry ->
                    val interceptIdentifier =
                        detailEntry.arguments?.getString(BurpRemoteRoute.INTERCEPT_IDENTIFIER_ARGUMENT)
                    if (interceptIdentifier != null) {
                        InterceptDetailRoute(
                            interceptIdentifier = interceptIdentifier,
                            interceptRepository = navigationDependencies.interceptRepository,
                            technicalLog = technicalLog,
                        )
                    }
                }
                composable(BurpRemoteRoute.LIVE_REPEATER) {
                    RepeaterRoute(
                        dashboardRepository = navigationDependencies.dashboardRepository,
                        technicalLog = technicalLog,
                    )
                }

                composable(
                    route = BurpRemoteRoute.SHARING,
                    arguments =
                        listOf(
                            navArgument(BurpRemoteRoute.HISTORY_IDENTIFIER_ARGUMENT) { type = NavType.StringType },
                        ),
                ) { sharingEntry ->
                    val historyIdentifier =
                        sharingEntry.arguments?.getString(BurpRemoteRoute.HISTORY_IDENTIFIER_ARGUMENT)
                    if (historyIdentifier != null) {
                        SharingRoute(
                            historyIdentifier = historyIdentifier,
                            historyRepository = navigationDependencies.historyRepository,
                            technicalLog = technicalLog,
                        )
                    }
                }

                composable(BurpRemoteRoute.SETTINGS_SECTION) {
                    SectionMenuScreen(
                        entries = TopLevelSection.Settings.indexEntries,
                        onOpenRoute = navController::openRoute,
                    )
                }
                // Burp 连接屏的扫码入口由壳的弹窗承担，这里只留地址端口与手输配对文本。
                composable(BurpRemoteRoute.SETTINGS_BURP_CONNECTION) {
                    BurpConnectionRoute(
                        connectionState = connectionState,
                        settingsRepository = navigationDependencies.settingsRepository,
                        remotePairingCoordinator = navigationDependencies.remotePairingCoordinator,
                        remoteControlClient = navigationDependencies.remoteControlClient,
                        technicalLog = technicalLog,
                    )
                }
                // feature:settings 的 SettingsRoute 是外观（主题）屏，对应 plan §43 的 Appearance。
                composable(BurpRemoteRoute.SETTINGS_APPEARANCE) {
                    SettingsRoute(
                        settingsRepository = navigationDependencies.settingsRepository,
                        technicalLog = technicalLog,
                    )
                }
                composable(BurpRemoteRoute.SETTINGS_LANGUAGE) {
                    LanguageRoute(technicalLog = technicalLog)
                }
                composable(BurpRemoteRoute.SETTINGS_SECURITY) { SecurityScreen() }
                composable(BurpRemoteRoute.SETTINGS_STORAGE) { StorageScreen() }
            }
        }

        if (isScannerOpen) {
            // 弹窗容器由壳提供（它盖住顶栏与底栏），取景、手输与本地校验由 feature:connection 提供。
            PairingScannerDialogLayer(
                conclusion = pairingConclusion,
                isPairingInFlight = isPairingInFlight,
                onDismiss = {
                    technicalLog.record(
                        TechnicalLogEvent(
                            category = TechnicalLogCategory.Pairing,
                            message = "扫码弹窗关闭",
                        ),
                    )
                    isScannerOpen = false
                },
                onScannedTicketText = submitTicket,
            )
        }
    }
}

// 连接状态在界面上只有一种来源；各屏读的是同一份，不各自去问传输层。
private fun connectionStateOf(navigationDependencies: NavigationDependencies): Flow<ConnectionState> =
    navigationDependencies.dashboardRepository.observeDashboardSummary().map { summary -> summary.connectionState }

/** 连的是哪台机器；还没配过端点时这一行留空，不编一个地址出来。 */
private fun endpointTextOf(endpoint: RemoteServerEndpoint?): String? =
    endpoint?.let { value -> "${value.host}:${value.port}" }

/** 顶栏写副标题的唯一一条路由；其余屏没有「连在哪台机器上」这件事要说。 */
private val CONNECTION_ROUTES: Set<String> =
    setOf(
        BurpRemoteRoute.SETTINGS_BURP_CONNECTION,
    )

/** 三个一级目的地：只有它们配底栏，也只有它们不显示返回箭头。 */
private val PRIMARY_DESTINATIONS: Set<String> =
    setOf(
        BurpRemoteRoute.DASHBOARD,
        BurpRemoteRoute.LIVE_SECTION,
        BurpRemoteRoute.SETTINGS_SECTION,
    )

/**
 * 顶栏标题：取当前目的地。带参数的详情页与列表页同属一件事，因此共用同一个标题。
 *
 * 分区与条目的文案住在 :ui，而本仓库开了非传递 R 类，本模块的 R 里没有它们，因此显式指名来源。
 */
@StringRes
private fun titleResourceOf(route: String?): Int =
    when (route) {
        BurpRemoteRoute.DASHBOARD -> UserInterfaceResources.string.navigation_section_dashboard
        BurpRemoteRoute.LIVE_SECTION -> UserInterfaceResources.string.navigation_section_live
        BurpRemoteRoute.LIVE_HISTORY,
        BurpRemoteRoute.LIVE_HISTORY_DETAIL,
        -> UserInterfaceResources.string.navigation_entry_live_history

        BurpRemoteRoute.LIVE_INTERCEPT,
        BurpRemoteRoute.LIVE_INTERCEPT_DETAIL,
        -> UserInterfaceResources.string.navigation_entry_live_intercept

        BurpRemoteRoute.LIVE_REPEATER -> UserInterfaceResources.string.navigation_entry_live_repeater
        BurpRemoteRoute.SHARING -> R.string.navigation_title_sharing
        BurpRemoteRoute.SETTINGS_SECTION -> UserInterfaceResources.string.navigation_section_settings
        BurpRemoteRoute.SETTINGS_BURP_CONNECTION ->
            UserInterfaceResources.string.navigation_entry_settings_burp_connection

        BurpRemoteRoute.SETTINGS_APPEARANCE -> UserInterfaceResources.string.navigation_entry_settings_appearance
        BurpRemoteRoute.SETTINGS_LANGUAGE -> UserInterfaceResources.string.navigation_entry_settings_language
        BurpRemoteRoute.SETTINGS_SECURITY -> UserInterfaceResources.string.navigation_entry_settings_security
        BurpRemoteRoute.SETTINGS_STORAGE -> UserInterfaceResources.string.navigation_entry_settings_storage
        else -> UserInterfaceResources.string.navigation_section_dashboard
    }

// 只认一级与索引屏：带参数的路由需要参数值，从额外参数把应用指过去会直接崩在导航图上。
// 扫码不再占路由（它是壳的弹窗），因此这里没有它的位置。
private val SUPPORTED_START_ROUTES: Set<String> =
    setOf(
        BurpRemoteRoute.DASHBOARD,
        BurpRemoteRoute.LIVE_SECTION,
        BurpRemoteRoute.LIVE_HISTORY,
        BurpRemoteRoute.LIVE_INTERCEPT,
        BurpRemoteRoute.LIVE_REPEATER,
        BurpRemoteRoute.SETTINGS_SECTION,
        BurpRemoteRoute.SETTINGS_BURP_CONNECTION,
        BurpRemoteRoute.SETTINGS_APPEARANCE,
        BurpRemoteRoute.SETTINGS_LANGUAGE,
        BurpRemoteRoute.SETTINGS_SECURITY,
        BurpRemoteRoute.SETTINGS_STORAGE,
    )

// 调试注入的票据不再改落地屏：配对入口与扫码相同，落地屏照旧由 startRoute 决定。
private fun resolveStartRoute(requestedRoute: String?): String =
    if (requestedRoute != null && SUPPORTED_START_ROUTES.contains(requestedRoute)) {
        requestedRoute
    } else {
        BurpRemoteRoute.DASHBOARD
    }

private fun NavController.openRoute(route: String) {
    // 连点同一个入口不该把同一个屏叠进返回栈，否则要按很多次返回键才出得去。
    if (currentDestination?.route == route) return
    navigate(route) { launchSingleTop = true }
}
