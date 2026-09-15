package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeFlavor
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/** 设置界面的状态。命名遵循 rules.md §8.2，不用 UiState 这种缩写。 */
data class SettingsUserInterfaceState(
    val themeMode: ThemeMode = ThemeMode.Automatic,
    val themeFlavor: ThemeFlavor = ThemeFlavor.BurpClassic,
)
