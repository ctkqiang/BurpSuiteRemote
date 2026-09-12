package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/** 设置界面的状态持有者。写操作的唯一入口是 [selectThemeMode]，界面不直接碰仓库。 */
class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        settingsRepository
            .observeThemeMode()
            .map { themeMode -> SettingsUiState(themeMode) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = SettingsUiState(),
            )

    fun selectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(themeMode) }
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
