package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 深色配色。
 *
 * 底色是近黑中性色而不是纯黑：纯黑会让相邻的层级差不可见，也会让 OLED 上的滚动拖影更明显。
 */
val BurpRemoteDarkColourScheme =
    BurpRemoteColourScheme(
        background = Color(0xFF0E1114),
        surface = Color(0xFF171B20),
        surfaceElevated = Color(0xFF1E232A),
        outline = Color(0xFF2A3138),
        contentPrimary = Color(0xFFECEEF1),
        contentSecondary = Color(0xFF9AA3AC),
        accent = BurpRemoteColour,
        onAccent = Color(0xFF14171C),
        success = Color(0xFF3DBB74),
        warning = Color(0xFFE0A030),
        danger = Color(0xFFF0665A),
        information = Color(0xFF52A8E8),
        scrim = Color(0x8C000000),
    )
