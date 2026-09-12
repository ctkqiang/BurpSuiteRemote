package xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * 本机设置的读写端口：外观，以及与插件之间的连接配置（plan §47）。
 *
 * 领域层只认这个接口；DataStore、文件、内存实现都藏在后面，所以领域层不知道偏好存在哪，
 * 也不知道设备身份是不是被加密过（rules.md §6.1）。
 */
interface SettingsRepository {
    /** 外观主题。 */
    fun observeThemeMode(): Flow<ThemeMode>

    /** 写外观主题。 */
    suspend fun setThemeMode(themeMode: ThemeMode)

    /** 插件端点的地址与端口；从未配置过时为 null，界面据此写「尚未配置」。 */
    fun observeServerEndpoint(): Flow<RemoteServerEndpoint?>

    /** 写插件端点；这是用户在设置里手改地址端口的唯一入口。 */
    suspend fun saveServerEndpoint(endpoint: RemoteServerEndpoint)

    /** 抹掉插件端点，但保留设备身份：两者是两件事，换台机器连不代表要重新配对。 */
    suspend fun clearServerEndpoint()

    /** 本机是否已经拿到插件签发的设备身份。 */
    fun observeIsPaired(): Flow<Boolean>

    /** 清除配对：抹掉设备身份与保存的端点，回到未配对状态。 */
    suspend fun clearPairing()
}
