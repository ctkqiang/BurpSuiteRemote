package xin.ctkqiang.burpsuite.remote.mobileapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive.ArchiveRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection.BurpConnectionRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard.DashboardRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.history.HistoryDetailRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.history.HistoryRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept.InterceptDetailRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept.InterceptRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater.RepeaterRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.screenshot.ScreenshotScreen
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguageRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.SecurityScreen
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.SettingsRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.StorageScreen
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing.SharingRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.BurpRemoteNavigationBar
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.BurpRemoteRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.NavigationDependencies
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.SectionMenuScreen
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.TopLevelSection

/**
 * 应用导航壳：底栏加各屏装配（plan §43）。
 *
 * NavHost 只能建在这一层：feature 模块不认识 NavController，只接受回调（rules.md §6.1）。
 * 壳只认 [NavigationDependencies]，因此「哪一屏能做到什么」在编译期就定死了。
 */
@Composable
fun BurpsuiteRemoteNavigationHost(
    navigationDependencies: NavigationDependencies,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedSection =
        TopLevelSection.entries.firstOrNull { section -> section.containsRoute(currentRoute) }

    // 连接状态在界面上只有一种来源；各屏读的是同一份，不各自去问传输层。
    val connectionState: Flow<ConnectionState> =
        remember(navigationDependencies) {
            navigationDependencies.dashboardRepository.observeDashboardSummary().map { summary ->
                summary.connectionState
            }
        }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BurpRemoteNavigationBar(
                selectedSection = selectedSection,
                onSectionSelected = { section -> navController.openRoute(section.landingRoute) },
            )
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = BurpRemoteRoute.DASHBOARD,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(BurpRemoteRoute.DASHBOARD) {
                DashboardRoute(
                    dashboardRepository = navigationDependencies.dashboardRepository,
                    historyRepository = navigationDependencies.historyRepository,
                    onOpenLiveHistory = { navController.openRoute(BurpRemoteRoute.LIVE_HISTORY) },
                    onOpenLiveIntercept = { navController.openRoute(BurpRemoteRoute.LIVE_INTERCEPT) },
                    onOpenLiveRepeater = { navController.openRoute(BurpRemoteRoute.LIVE_REPEATER) },
                    onOpenHistoryRecord = { historyIdentifier ->
                        navController.openRoute(BurpRemoteRoute.liveHistoryDetail(historyIdentifier))
                    },
                )
            }

            composable(BurpRemoteRoute.LIVE_SECTION) {
                SectionMenuScreen(
                    titleResource = TopLevelSection.Live.labelResource,
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
                    )
                }
            }
            composable(BurpRemoteRoute.LIVE_INTERCEPT) {
                InterceptRoute(
                    interceptRepository = navigationDependencies.interceptRepository,
                    onOpenInterceptRecord = { interceptIdentifier ->
                        navController.openRoute(BurpRemoteRoute.liveInterceptDetail(interceptIdentifier))
                    },
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
                    )
                }
            }
            composable(BurpRemoteRoute.LIVE_REPEATER) {
                RepeaterRoute(dashboardRepository = navigationDependencies.dashboardRepository)
            }

            composable(BurpRemoteRoute.ARCHIVE_SECTION) {
                ArchiveRoute(
                    historyRepository = navigationDependencies.historyRepository,
                    onOpenHistoryRecord = { historyIdentifier ->
                        navController.openRoute(BurpRemoteRoute.liveHistoryDetail(historyIdentifier))
                    },
                    onOpenSharing = { identifier -> navController.openRoute(BurpRemoteRoute.sharing(identifier)) },
                    onOpenScreenshots = { navController.openRoute(BurpRemoteRoute.ARCHIVE_SCREENSHOTS) },
                )
            }
            composable(BurpRemoteRoute.ARCHIVE_SCREENSHOTS) { ScreenshotScreen() }

            composable(
                route = BurpRemoteRoute.SHARING,
                arguments =
                    listOf(
                        navArgument(BurpRemoteRoute.HISTORY_IDENTIFIER_ARGUMENT) { type = NavType.StringType },
                    ),
            ) { sharingEntry ->
                val historyIdentifier = sharingEntry.arguments?.getString(BurpRemoteRoute.HISTORY_IDENTIFIER_ARGUMENT)
                if (historyIdentifier != null) {
                    SharingRoute(
                        historyIdentifier = historyIdentifier,
                        historyRepository = navigationDependencies.historyRepository,
                    )
                }
            }

            composable(BurpRemoteRoute.SETTINGS_SECTION) {
                SectionMenuScreen(
                    titleResource = TopLevelSection.Settings.labelResource,
                    entries = TopLevelSection.Settings.indexEntries,
                    onOpenRoute = navController::openRoute,
                )
            }
            composable(BurpRemoteRoute.SETTINGS_BURP_CONNECTION) {
                BurpConnectionRoute(
                    connectionState = connectionState,
                    remoteControlClient = navigationDependencies.remoteControlClient,
                )
            }
            // feature:settings 的 SettingsRoute 是外观（主题）屏，对应 plan §43 的 Appearance。
            composable(BurpRemoteRoute.SETTINGS_APPEARANCE) {
                SettingsRoute(
                    settingsRepository = navigationDependencies.settingsRepository,
                )
            }
            composable(BurpRemoteRoute.SETTINGS_LANGUAGE) { LanguageRoute() }
            composable(BurpRemoteRoute.SETTINGS_SECURITY) { SecurityScreen() }
            composable(BurpRemoteRoute.SETTINGS_STORAGE) { StorageScreen() }
        }
    }
}

private fun NavController.openRoute(route: String) {
    // 连点同一个入口不该把同一个屏叠进返回栈，否则要按很多次返回键才出得去。
    if (currentDestination?.route == route) return
    navigate(route) { launchSingleTop = true }
}
