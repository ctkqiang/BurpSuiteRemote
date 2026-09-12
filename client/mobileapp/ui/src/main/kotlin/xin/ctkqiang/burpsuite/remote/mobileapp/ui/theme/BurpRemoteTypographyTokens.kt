package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 五档字号的具体取值；深浅两套主题共用，字色由各自的主题在调用处贴合。 */
val BurpRemoteTypographyTokens =
    BurpRemoteTypography(
        display = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
        title = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
        body = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
        label = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        technical = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp),
    )
