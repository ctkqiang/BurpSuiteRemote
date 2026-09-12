package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

/**
 * 用户在语言设置上的意图。
 *
 * 界面把选择包成意图，ViewModel 负责落盘并请求重建——这样「选项被点了」和「界面换语言了」
 * 是两件事，将来加确认弹窗时不用动界面。
 */
sealed interface LanguageUserInterfaceIntent {
    /** 选择一种语言。 */
    data class SelectLanguage(val language: LanguagePreference) : LanguageUserInterfaceIntent
}
