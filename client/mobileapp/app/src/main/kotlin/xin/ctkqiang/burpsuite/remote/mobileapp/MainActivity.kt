package xin.ctkqiang.burpsuite.remote.mobileapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.emptyFlow
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
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
        ConnectionSessionEffect(navigationDependencies = navigationDependencies)
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

/**
 * 让会话前台服务跟着会话走：会话离开「未连接」就把它拉起来，回到「未连接」就把它停掉。
 *
 * 启停都放在这里而不是装配容器里，原因是 Android 12 起禁止从后台启动前台服务。这个组合函数
 * 只有在 Activity 已经进入前台、并且主题偏好读出来之后才会被组合，因此它是唯一安全的启动点；
 * 放进容器的话，进程被广播之类的路径拉起来时会直接抛 ForegroundServiceStartNotAllowedException。
 *
 * 通知权限只在这里问、而且只在会话真的建立起来时问：冷启动就问「要不要发通知」，用户还没有
 * 任何上下文去判断该不该给。
 */
@Composable
private fun ConnectionSessionEffect(navigationDependencies: NavigationDependencies) {
    val context = LocalContext.current
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }

    // 没有远程控制端口时拿一条空流顶上，这样下面几个可组合调用在任何一次组合里的位置都固定，
    // 不会因为端口有无而改变槽位结构。
    val connectionStateFlow = navigationDependencies.remoteControlClient?.connectionState ?: emptyFlow()
    val connectionState by connectionStateFlow.collectAsState(initial = ConnectionState.Disconnected)

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.Disconnected) {
            context.stopService(ConnectionSessionService.stopIntent(context))
            return@LaunchedEffect
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.canPostNotifications()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        ConnectionSessionNotification.ensureChannel(context)
        ContextCompat.startForegroundService(
            context,
            ConnectionSessionService.startIntent(context = context, state = connectionState),
        )
    }
}

// 33 之前通知不需要授权；33 起要显式检查，否则那条常驻通知会被系统静默丢掉。
private fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
