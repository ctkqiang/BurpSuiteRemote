package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 深色配色。
 *
 * 底色是纯黑：OLED 上不发光的那一档最省电，也让等宽技术值在一片黑里对比度最高。
 * 纯黑的代价是邻近层级不可见，因此卡片与底栏各抬一档灰（`#0E0F11`、`#17181B`）并配 1dp 描边，
 * 层级靠「抬起来的灰 + 描边」表达，不靠阴影——纯黑底上的阴影本来就看不见。
 *
 * 语义色在这套底上各提亮一档：原色在纯黑上对比度不足 4.5:1，读起来是灰的。
 */
val BurpRemoteDarkColourScheme =
    BurpRemoteColourScheme(
        background = Color(0xFF000000),
        surface = Color(0xFF0E0F11),
        surfaceElevated = Color(0xFF17181B),
        outline = Color(0xFF26272B),
        contentPrimary = Color(0xFFF2F4F6),
        contentSecondary = Color(0xFF9AA1A9),
        accent = BurpRemoteColour,
        onAccent = Color(0xFF000000),
        success = Color(0xFF4ADE80),
        warning = Color(0xFFFBBF24),
        danger = Color(0xFFF87171),
        information = Color(0xFF60A5FA),
        scrim = Color(0xCC000000),
        onScrim = Color(0xFFFFFFFF),
        codeKey = Color(0xFFC4B5FD),
        codeString = Color(0xFF86EFAC),
        codeNumber = Color(0xFFFCD34D),
        codeLiteral = Color(0xFF67E8F9),
        codeComment = Color(0xFF7D8590),
    )
