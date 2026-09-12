package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import kotlinx.coroutines.flow.Flow

/**
 * 界面语言的读写端口。
 *
 * 领域的 `SettingsRepository` 这一轮只有主题，语言又必须在界面重建之前就能同步读到（见
 * [readPersistedLanguage]），因此单独放在这里；实现由装配层提供（rules.md §6.2）。
 */
interface LanguagePreferenceRepository {
    /** 观察语言偏好。 */
    fun observeLanguage(): Flow<LanguagePreference>

    /** 写入语言偏好。 */
    suspend fun setLanguage(language: LanguagePreference)

    /**
     * 同步读出当前语言。
     *
     * Activity 要在建界面之前定下 Configuration，那时协程还没跑起来，因此这一步必须是同步的。
     */
    fun readPersistedLanguage(): LanguagePreference
}
