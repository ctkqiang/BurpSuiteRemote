package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 带色短标签：整块底色压得很低，文字用颜色本身。
 *
 * 这是全应用唯一一处画胶囊标签的地方。[BurpRemoteStatusPill] 与日志级别标签都只是它的
 * 一次调用——同一件事有两种画法，两处迟早会漂移成两个样子。
 *
 * @param text 标签文本；调用方负责把它压到一行以内。
 * @param colour 标签的主色；底色与描边由它按固定比例推出来。
 * @param modifier 由调用方决定摆放。
 */
@Composable
fun BurpRemoteTintedLabel(
    text: String,
    colour: Color,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val shape = RoundedCornerShape(BurpRemoteRadius.Capsule)

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(color = colour.copy(alpha = FILL_ALPHA), shape = shape)
                .border(width = 1.dp, color = colour.copy(alpha = BORDER_ALPHA), shape = shape)
                .padding(horizontal = BurpRemoteSpacing.Medium, vertical = BurpRemoteSpacing.ExtraSmall),
    ) {
        BurpRemoteText(
            text = text,
            style = tokens.typography.label,
            colour = colour,
            maxLines = 1,
        )
    }
}

// 底色够淡才压得住正文，描边比底色略重才看得出边界。
private const val FILL_ALPHA = 0.16f
private const val BORDER_ALPHA = 0.4f
