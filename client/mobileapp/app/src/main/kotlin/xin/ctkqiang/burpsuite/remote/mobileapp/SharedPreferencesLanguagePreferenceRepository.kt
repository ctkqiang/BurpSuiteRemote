package xin.ctkqiang.burpsuite.remote.mobileapp

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguagePreference
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguagePreferenceRepository

/**
 * 用共享偏好存界面语言。
 *
 * 不用 DataStore 是因为 Activity 在 `attachBaseContext` 里就要同步读到语言，那时协程还没起来；
 * 共享偏好的首读是同步的，正好对上这个时机。写入立刻落到内存并异步刷盘，重建后还是同一个值。
 */
class SharedPreferencesLanguagePreferenceRepository(context: Context) : LanguagePreferenceRepository {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(LANGUAGE_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private val observedLanguage =
        MutableStateFlow(LanguagePreference.fromStorageValue(preferences.getString(LANGUAGE_KEY, null)))

    override fun observeLanguage(): Flow<LanguagePreference> =
        // onStart 里补发一次当前值：调用方订阅时偏好可能已经被别处改过。
        observedLanguage.onStart { emit(readPersistedLanguage()) }

    override suspend fun setLanguage(language: LanguagePreference) {
        preferences.edit().putString(LANGUAGE_KEY, language.storageValue).apply()
        observedLanguage.value = language
    }

    override fun readPersistedLanguage(): LanguagePreference =
        LanguagePreference.fromStorageValue(preferences.getString(LANGUAGE_KEY, null))

    private companion object {
        const val LANGUAGE_PREFERENCES_FILE_NAME = "language_preference"

        const val LANGUAGE_KEY = "language"
    }
}
