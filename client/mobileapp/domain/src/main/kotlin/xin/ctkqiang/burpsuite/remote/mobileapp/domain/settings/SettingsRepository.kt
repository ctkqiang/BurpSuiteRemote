package xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * 设置的读写端口。
 *
 * 领域层只认这个接口；DataStore、文件、内存实现都藏在后面，所以领域层不知道偏好存在哪。
 */
interface SettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(themeMode: ThemeMode)
}
