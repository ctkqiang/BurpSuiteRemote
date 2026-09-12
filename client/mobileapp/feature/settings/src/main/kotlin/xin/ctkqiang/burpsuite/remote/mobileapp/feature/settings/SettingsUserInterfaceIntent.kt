package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode

/**
 * 用户意图。界面只能通过它表达「用户想干什么」，不能自己直接改仓库（rules.md §8.1）。
 *
 * 界面把选择包成意图，ViewModel 负责把它翻成一次仓储写入——这样「选项被点了」和「偏好被改了」
 * 是两件事，将来加确认弹窗或撤销时不用动界面。
 */
sealed interface SettingsUserInterfaceIntent {
    data class SelectThemeMode(val themeMode: ThemeMode) : SettingsUserInterfaceIntent
}
