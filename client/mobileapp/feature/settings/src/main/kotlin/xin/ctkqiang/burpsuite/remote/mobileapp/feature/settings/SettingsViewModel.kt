package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/** 设置界面的状态持有者。写操作的唯一入口是 [handleIntent]，界面不直接碰仓库。 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) : ViewModel() {
    val uiState: StateFlow<SettingsUserInterfaceState> =
        settingsRepository
            .observeThemeMode()
            .map { themeMode -> SettingsUserInterfaceState(themeMode) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = SettingsUserInterfaceState(),
            )

    fun handleIntent(intent: SettingsUserInterfaceIntent) {
        when (intent) {
            is SettingsUserInterfaceIntent.SelectThemeMode -> selectThemeMode(intent.themeMode)
        }
    }

    private fun selectThemeMode(themeMode: ThemeMode) {
        record(
            message = "主题改为 ${themeMode.storageValue}",
            attributes = mapOf("themeMode" to themeMode.storageValue),
        )
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }

    private fun record(
        message: String,
        attributes: Map<String, String> = emptyMap(),
    ) {
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.UserInterface,
                message = message,
                attributes = attributes,
            ),
        )
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
