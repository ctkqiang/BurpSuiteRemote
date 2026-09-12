package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/** 设置界面的状态。界面只读这一份，不自己再存一份。 */
data class SettingsUiState(val themeMode: ThemeMode = ThemeMode.Automatic)
