package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 浅色配色。
 *
 * 底色是纯白：这是阅读密集界面的前提，任何带灰的「白」都会让等宽技术值看起来发脏。
 * 表面比底色略冷，靠明度差分层，不靠描边。
 */
val BurpRemoteLightColourScheme =
    BurpRemoteColourScheme(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF5F6F8),
        surfaceElevated = Color(0xFFFCFCFD),
        outline = Color(0xFFE1E5EA),
        contentPrimary = Color(0xFF14171C),
        contentSecondary = Color(0xFF5A6169),
        accent = BurpRemoteColour,
        onAccent = Color(0xFFFFFFFF),
        success = Color(0xFF1B7F4B),
        warning = Color(0xFFB26A00),
        danger = Color(0xFFC0392B),
        information = Color(0xFF1D6FB8),
        scrim = Color(0x5214171C),
    )
