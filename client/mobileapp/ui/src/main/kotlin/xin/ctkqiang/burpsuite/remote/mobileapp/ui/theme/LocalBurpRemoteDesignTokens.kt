package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 当前主题的下发通道。
 *
 * 默认值是浅色而不是 `error(...)`：@Preview 与还没被主题包住的界面片段也要能画出东西，
 * 让一次预览失败看起来像一次崩溃并不划算。
 */
val LocalBurpRemoteDesignTokens =
    staticCompositionLocalOf {
        BurpRemoteDesignTokens(
            colourScheme = BurpRemoteLightColourScheme,
            typography = BurpRemoteTypographyTokens,
            isDark = false,
        )
    }
