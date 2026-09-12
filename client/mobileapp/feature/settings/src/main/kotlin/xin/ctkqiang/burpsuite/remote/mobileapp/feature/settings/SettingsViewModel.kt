package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository

/** 设置界面的状态持有者。写操作的唯一入口是 [handleIntent]，界面不直接碰仓库。 */
class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
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
            is SettingsUserInterfaceIntent.SelectThemeMode ->
                viewModelScope.launch { settingsRepository.setThemeMode(intent.themeMode) }
        }
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
