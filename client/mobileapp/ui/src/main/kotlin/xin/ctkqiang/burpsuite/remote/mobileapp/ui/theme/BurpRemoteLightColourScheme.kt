package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 浅色配色。
 *
 * 底色是纯白：这是阅读密集界面的前提，任何带灰的「白」都会让等宽技术值看起来发脏。
 * 卡片与页面只差一档极浅的灰（`#FAFAFA`），分层主要靠 1dp 描边，不叠阴影——一屏里叠三层阴影之后，
 * 哪一层更靠前就全靠猜了。底栏与弹层用与页面相同的纯白，靠描边与投影从内容里浮起来。
 */
val BurpRemoteLightColourScheme =
    BurpRemoteColourScheme(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFAFAFA),
        surfaceElevated = Color(0xFFFFFFFF),
        outline = Color(0xFFE7E8EA),
        contentPrimary = Color(0xFF0B0D0F),
        contentSecondary = Color(0xFF5B6169),
        accent = BurpRemoteColour,
        onAccent = Color(0xFFFFFFFF),
        success = Color(0xFF16A34A),
        warning = Color(0xFFD97706),
        danger = Color(0xFFDC2626),
        information = Color(0xFF2563EB),
        scrim = Color(0xB3000000),
        onScrim = Color(0xFFFFFFFF),
        codeKey = Color(0xFF7C3AED),
        codeString = Color(0xFF15803D),
        codeNumber = Color(0xFFB45309),
        codeLiteral = Color(0xFF0E7490),
        codeComment = Color(0xFF6B7280),
    )
