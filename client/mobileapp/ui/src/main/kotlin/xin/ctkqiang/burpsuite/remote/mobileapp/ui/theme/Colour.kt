package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 品牌橙。
 *
 * 全项目只有这一处写死色值，其余颜色要么由它派生、要么取 Material 3 的语义色。
 * 这个取值是本项目选定的，不保证与官方素材逐位一致——要完全对齐只改这一行。
 */
val BurpOrange = Color(0xFFFF6633)

internal val LightColours = lightColorScheme(primary = BurpOrange, onPrimary = Color.White)

internal val DarkColours = darkColorScheme(primary = BurpOrange, onPrimary = Color.Black)
