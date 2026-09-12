package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository

/** 装配层：把仓库交给 ViewModel，把状态交给无状态的界面。 */
@Composable
fun SettingsRoute(settingsRepository: SettingsRepository) {
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(settingsRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(uiState = uiState, onThemeModeSelected = viewModel::selectThemeMode)
}
