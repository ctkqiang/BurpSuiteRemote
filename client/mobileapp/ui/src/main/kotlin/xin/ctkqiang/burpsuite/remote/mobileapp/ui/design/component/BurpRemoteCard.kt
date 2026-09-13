package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 卡片：一组相关信息的容器。
 *
 * 分层靠 1dp 描边加表面色，不靠阴影：一屏里叠三层阴影之后，哪一层更靠前就全靠猜了。
 * [content] 放在最后：卡片永远以尾随 lambda 的形式被调用，参数顺序反了的话每个调用点都得写参数名。
 * 可交互的卡片必须给 [onClick]，只把 [isInteractive] 打开而没有点击行为等于骗用户点一下试试。
 */
@Composable
fun BurpRemoteCard(
    isInteractive: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()
    val shape = RoundedCornerShape(BurpRemoteRadius.Card)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // 按下的那一刻整张卡轻微内缩：手指落下与卡片响应之间因此有了因果，而不是「点完才知道生效了」。
    val pressScale by
        animateFloatAsState(
            targetValue = if (isPressed) PRESSED_SCALE else RESTING_SCALE,
            animationSpec = tween(BurpRemoteMotion.DURATION_FAST, easing = BurpRemoteMotion.EasingStandard),
            label = "card-press-scale",
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(shape)
                .background(color = tokens.colourScheme.surface, shape = shape)
                .border(width = 1.dp, color = tokens.colourScheme.outline, shape = shape)
                .then(
                    if (isInteractive && onClick != null) {
                        // 涟漪关掉：这一档反馈由缩放与触感承担，两套反馈同时出现会互相打架。
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            haptics.tap()
                            onClick()
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(BurpRemoteSpacing.Large),
        content = content,
    )
}

private const val PRESSED_SCALE = 0.985f
private const val RESTING_SCALE = 1f
