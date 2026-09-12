package xin.ctkqiang.burpsuite.remote.mobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings.SettingsRoute
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as RemoteControlApplication).container
        setContent { RemoteControlRoot(settingsRepository = container.settingsRepository) }
    }
}

@Composable
private fun RemoteControlRoot(settingsRepository: SettingsRepository) {
    // 主题偏好读出来之前不画内容：先按默认主题画一帧再切，用户会看到一次闪白或闪黑。
    val themeMode by
        produceState<ThemeMode?>(initialValue = null, settingsRepository) {
            settingsRepository.observeThemeMode().collect { observedThemeMode -> value = observedThemeMode }
        }

    val resolvedThemeMode = themeMode ?: return

    BurpsuiteRemoteTheme(themeMode = resolvedThemeMode) {
        SettingsRoute(settingsRepository = settingsRepository)
    }
}
