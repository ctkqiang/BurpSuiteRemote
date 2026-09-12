package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

/**
 * 一次主题解析的全部结果。
 *
 * 调色板与字阶一起下发：控件若自己去取字阶，深色下就没人负责把字色换成对应的那一档。
 *
 * @property colourScheme 本次解析出的配色。
 * @property typography 本次解析出的字阶，深浅两套共用一份取值。
 * @property isDark 当前是否为深色主题；系统栏图标明暗、真模糊可用性这类判断读它。
 */
data class BurpRemoteDesignTokens(
    val colourScheme: BurpRemoteColourScheme,
    val typography: BurpRemoteTypography,
    val isDark: Boolean,
)
