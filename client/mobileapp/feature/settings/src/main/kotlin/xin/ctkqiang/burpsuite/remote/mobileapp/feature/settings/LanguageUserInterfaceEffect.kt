package xin.ctkqiang.burpsuite.remote.mobileapp.feature.settings

/**
 * 语言设置发出的一次性效果。
 *
 * 换了语言必须重建 Activity，界面上的文字才会跟着变；重建是一次性动作，进状态会在转屏后重放
 * （rules.md §8.1）。
 */
sealed interface LanguageUserInterfaceEffect {
    /** 请求装配层重建界面，让新的语言生效。 */
    data object RestartForLanguageChange : LanguageUserInterfaceEffect
}
