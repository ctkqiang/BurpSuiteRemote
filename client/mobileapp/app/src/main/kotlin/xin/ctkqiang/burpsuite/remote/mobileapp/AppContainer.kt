package xin.ctkqiang.burpsuite.remote.mobileapp

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import xin.ctkqiang.burpsuite.remote.mobileapp.data.database.BurpRemoteDatabase
import xin.ctkqiang.burpsuite.remote.mobileapp.data.repository.ProjectionDashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.data.repository.RoomHistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.data.repository.RoomInterceptRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.DataStoreSettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.NavigationDependencies

/** 依赖装配处。全应用只有这里知道接口背后的实现是谁，也只有这里碰得到 Room。 */
class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    // 库里放的是事件日志与投影，属于敏感数据，留在应用私有目录（plan §58）。
    private val database: BurpRemoteDatabase =
        Room.databaseBuilder(applicationContext, BurpRemoteDatabase::class.java, DATABASE_FILE_NAME).build()

    /** 设置的读写端口。 */
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(applicationContext)

    /** 历史记录的读端口，读的是投影而不是事实源。 */
    val historyRepository: HistoryRepository = RoomHistoryRepository(database.historyRecordTable())

    /** 拦截项的读端口。 */
    val interceptRepository: InterceptRepository = RoomInterceptRepository(database.interceptRecordTable())

    // 远程传输层还没有适配器（data 里没有 RemoteControlClient 的实现），连接状态恒为「未连接」：
    // 界面照实显示，而不是编一个假状态；适配器落地后换成它的连接状态流即可。
    val connectionState: Flow<ConnectionState> = flowOf(ConnectionState.Disconnected)

    /** 配对命令的接收方；适配器接上之前是空，连接屏据此把配对入口画成禁用并说明原因。 */
    val remoteControlClient: RemoteControlClient? = null

    /** 主面板汇总：由历史投影、拦截投影与连接状态三路合并而来。 */
    val dashboardRepository: DashboardRepository =
        ProjectionDashboardRepository(
            historyRepository = historyRepository,
            interceptRepository = interceptRepository,
            connectionState = connectionState,
        )

    /** 交给导航壳的依赖表；导航壳只认这一个入参。 */
    fun navigationDependencies(): NavigationDependencies =
        NavigationDependencies(
            dashboardRepository = dashboardRepository,
            historyRepository = historyRepository,
            interceptRepository = interceptRepository,
            settingsRepository = settingsRepository,
            remoteControlClient = remoteControlClient,
        )

    private companion object {
        const val DATABASE_FILE_NAME = "burp-remote.db"
    }
}
