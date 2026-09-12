package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 区块的出现与消失。
 *
 * 统一从这里走：在线/离线、空态与列表、错误说明都是用同一条时间线进出的，界面因此不会一处快一处慢。
 */
@Composable
fun BurpRemoteStateTransition(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter =
            fadeIn(
                animationSpec =
                    tween(BurpRemoteMotion.DURATION_REGULAR, easing = BurpRemoteMotion.EasingStandard),
            ) +
                expandVertically(
                    animationSpec =
                        tween(BurpRemoteMotion.DURATION_REGULAR, easing = BurpRemoteMotion.EasingStandard),
                ),
        exit =
            fadeOut(
                animationSpec =
                    tween(BurpRemoteMotion.DURATION_FAST, easing = BurpRemoteMotion.EasingStandard),
            ) +
                shrinkVertically(
                    animationSpec =
                        tween(BurpRemoteMotion.DURATION_FAST, easing = BurpRemoteMotion.EasingStandard),
                ),
    ) {
        content()
    }
}
