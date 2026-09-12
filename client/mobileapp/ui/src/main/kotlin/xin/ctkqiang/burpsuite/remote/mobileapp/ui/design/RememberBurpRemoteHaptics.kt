package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteHaptics

/**
 * 取当前窗口的触感封装。
 *
 * 放在设计系统入口包而不是主题包里：页面用它、组件也用它，主题包不该成为页面必须认识的一层。
 */
@Composable
fun rememberBurpRemoteHaptics(): BurpRemoteHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) { BurpRemoteHaptics(hapticFeedback) }
}
