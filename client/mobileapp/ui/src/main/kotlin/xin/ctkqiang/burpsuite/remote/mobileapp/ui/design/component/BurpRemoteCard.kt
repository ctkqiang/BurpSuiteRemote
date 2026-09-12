package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 卡片：一组相关信息的容器。
 *
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

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(color = tokens.colourScheme.surface, shape = shape)
                .border(
                    width = 1.dp,
                    color = tokens.colourScheme.outline.copy(alpha = CARD_BORDER_ALPHA),
                    shape = shape,
                )
                .then(
                    if (isInteractive && onClick != null) {
                        Modifier.clickable {
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

private const val CARD_BORDER_ALPHA = 0.7f
