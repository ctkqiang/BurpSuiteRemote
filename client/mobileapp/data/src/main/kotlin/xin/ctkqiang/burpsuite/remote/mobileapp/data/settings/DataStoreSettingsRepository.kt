package xin.ctkqiang.burpsuite.remote.mobileapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

// 放顶层而不是类内伴生对象里：下面的属性委托在类外，看不到类内的成员。
private const val SETTINGS_FILE_NAME = "settings"

private val Context.settingsDataStore by preferencesDataStore(name = SETTINGS_FILE_NAME)

/** DataStore 实现的设置端口。偏好文件在应用私有目录里，退出应用也不会丢。 */
class DataStoreSettingsRepository(private val context: Context) : SettingsRepository {
    override fun observeThemeMode(): Flow<ThemeMode> =
        context.settingsDataStore.data
            // 读盘失败（文件损坏、权限异常）时回落到空偏好，而不是让界面一直停在加载态。
            .catch { emit(emptyPreferences()) }
            .map { preferences -> ThemeMode.fromStorageValue(preferences[THEME_MODE_KEY]) }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.storageValue
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
