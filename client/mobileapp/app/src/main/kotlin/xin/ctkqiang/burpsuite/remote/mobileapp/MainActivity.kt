package xin.ctkqiang.burpsuite.remote.mobileapp

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguagePreference
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguagePreferenceRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LocalLanguagePreferenceRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.NavigationDependencies
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.util.Locale

/** 唯一 Activity。界面全部是 Compose，没有 XML 布局要托管。 */
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // 语言要在界面建立之前定下来，否则会先按系统语言画一帧再切；换语言后由 LanguageRoute 触发重建，
        // 重建会再走一次这里，于是新语言生效。
        val language =
            (newBase.applicationContext as? RemoteControlApplication)
                ?.languagePreferenceRepository
                ?.readPersistedLanguage()
                ?: LanguagePreference.Automatic
        super.attachBaseContext(newBase.withLanguage(language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = application as RemoteControlApplication
        setContent {
            RemoteControlRoot(
                navigationDependencies = application.container.navigationDependencies(),
                languagePreferenceRepository = application.languagePreferenceRepository,
            )
        }
    }
}

private fun Context.withLanguage(language: LanguagePreference): Context {
    val languageTag = language.languageTag ?: return this
    val locale = Locale.forLanguageTag(languageTag)
    // 全局默认语言也要跟着走：日期时间格式化器在构造时就读它，只改 Configuration 不够。
    Locale.setDefault(locale)
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList(locale))
    return createConfigurationContext(configuration)
}

@Composable
private fun RemoteControlRoot(
    navigationDependencies: NavigationDependencies,
    languagePreferenceRepository: LanguagePreferenceRepository,
) {
    // 主题偏好读出来之前不画内容：先按默认主题画一帧再切，用户会看到一次闪白或闪黑。
    val themeMode by
        produceState<ThemeMode?>(initialValue = null, navigationDependencies.settingsRepository) {
            navigationDependencies.settingsRepository.observeThemeMode().collect { observedThemeMode ->
                value = observedThemeMode
            }
        }

    val resolvedThemeMode = themeMode ?: return

    BurpsuiteRemoteTheme(themeMode = resolvedThemeMode) {
        CompositionLocalProvider(
            LocalLanguagePreferenceRepository provides languagePreferenceRepository,
        ) {
            BurpsuiteRemoteNavigationHost(navigationDependencies = navigationDependencies)
        }
    }
}
