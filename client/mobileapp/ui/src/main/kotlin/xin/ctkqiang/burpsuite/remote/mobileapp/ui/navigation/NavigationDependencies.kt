package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository

/**
 * 导航壳的全部依赖：五个领域端口、一个配对入口与一个日志端口，一个都不多。
 *
 * 各屏只拿得到这里列出的东西，因此「这一屏能做到什么」在编译期就定死了——
 * 缺少某个端口时界面只能如实说明缺什么，不能自己造数据（rules.md §5.1）。
 *
 * @property dashboardRepository 主面板汇总的读端口。
 * @property historyRepository 历史投影的读端口；实时与归档共用它，靠 archiveState 区分。
 * @property interceptRepository 拦截投影的读端口。
 * @property settingsRepository 偏好的读写端口：外观，以及插件地址端口与配对状态。
 * @property remoteControlClient 远程控制端口；未接通时为空，各屏据此显示 OFFLINE。
 * @property remotePairingCoordinator 配对入口；未接通时为空，连接屏据此把配对入口画成禁用。
 * @property technicalLog 技术日志端口；由壳分给各屏，各屏不再各自去拿全局日志。
 */
data class NavigationDependencies(
    val dashboardRepository: DashboardRepository,
    val historyRepository: HistoryRepository,
    val interceptRepository: InterceptRepository,
    val settingsRepository: SettingsRepository,
    val remoteControlClient: RemoteControlClient?,
    val remotePairingCoordinator: RemotePairingCoordinator? = null,
    val technicalLog: TechnicalLog = SilentTechnicalLog,
)
