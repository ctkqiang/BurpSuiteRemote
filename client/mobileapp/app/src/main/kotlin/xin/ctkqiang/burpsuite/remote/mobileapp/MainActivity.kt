package xin.ctkqiang.burpsuite.remote.mobileapp

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguagePreference
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LanguagePreferenceRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.LocalLanguagePreferenceRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.NavigationDependencies
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.SystemBarIconAppearance
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
        // 内容铺到系统栏下面，留白交给 WindowInsets；系统栏图标的明暗在主题里按主题切（plan §45）。
        // 系统栏自身的底色必须是透明的：默认那层半透明遮罩会在手势条上方压出一条比底栏浅的色带。
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // 系统给导航栏加的对比度遮罩同理，它会把深色主题底下的手势条区域提亮。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val application = application as RemoteControlApplication
        val startRoute = debugStartRouteExtra()
        val pairingTicketText = debugPairingTicketExtra()
        setContent {
            RemoteControlRoot(
                navigationDependencies = application.container.navigationDependencies(),
                languagePreferenceRepository = application.languagePreferenceRepository,
                startRoute = startRoute,
                pairingTicketText = pairingTicketText,
            )
        }
    }

    // 只对可在调试的包生效：发布版忽略这个额外参数，免得外部 Intent 把应用指到任意一屏。
    private fun debugStartRouteExtra(): String? {
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (isDebuggable) intent?.getStringExtra(EXTRA_START_ROUTE) else null
    }

    // 端到端联调入口：模拟器给不出真实二维码画面，因此允许调试包直接递一张票据进来；发布版忽略它。
    private fun debugPairingTicketExtra(): String? {
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (isDebuggable) intent?.getStringExtra(EXTRA_PAIRING_TICKET) else null
    }

    private companion object {
        const val EXTRA_START_ROUTE = "startRoute"
        const val EXTRA_PAIRING_TICKET = "pairingTicket"
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

// LocalContext 可能是被包过多层的 Context，直接强转 Activity 会失败。
private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
private fun RemoteControlRoot(
    navigationDependencies: NavigationDependencies,
    languagePreferenceRepository: LanguagePreferenceRepository,
    startRoute: String?,
    pairingTicketText: String?,
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
        ApplySystemBarIconAppearance()
        CompositionLocalProvider(
            LocalLanguagePreferenceRepository provides languagePreferenceRepository,
        ) {
            BurpsuiteRemoteNavigationHost(
                navigationDependencies = navigationDependencies,
                startRoute = startRoute,
                pairingTicketText = pairingTicketText,
            )
        }
    }
}

/**
 * 系统栏图标的明暗随主题切换。
 *
 * 浅色主题配浅色图标等于状态栏字直接消失，这是必须每次主题变化都重设一次的系统状态。
 */
@Composable
private fun ApplySystemBarIconAppearance() {
    val tokens = LocalBurpRemoteDesignTokens.current
    val view = LocalView.current
    val context = LocalContext.current
    if (view.isInEditMode) return

    val activity = context.findActivity() ?: return
    SideEffect {
        // 明暗映射是纯函数，可被单元测试断言；错法的表现是状态栏的字与底色同色。
        val appearance = SystemBarIconAppearance.of(isDarkTheme = tokens.isDark)
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = appearance.isLightStatusBar
            isAppearanceLightNavigationBars = appearance.isLightNavigationBar
        }
    }
}
