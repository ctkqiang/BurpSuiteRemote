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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 状态胶囊：LIVE / OFFLINE / 警告这类一眼判读的短标签。
 *
 * 底色用语义色压到很低的透明度，文字用语义色本身：既能把颜色带进列表，又不会盖过正文。
 */
@Composable
fun BurpRemoteStatusPill(
    text: String,
    tone: BurpRemoteStatusTone,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val toneColour = toneColourFor(tone, tokens.colourScheme)
    val shape = RoundedCornerShape(BurpRemoteRadius.Capsule)

    Box(
        modifier =
            Modifier
                .clip(shape)
                .background(color = toneColour.copy(alpha = PILL_FILL_ALPHA), shape = shape)
                .border(width = 1.dp, color = toneColour.copy(alpha = PILL_BORDER_ALPHA), shape = shape)
                .padding(horizontal = BurpRemoteSpacing.Small, vertical = BurpRemoteSpacing.ExtraSmall),
    ) {
        BurpRemoteText(
            text = text,
            style = tokens.typography.label,
            colour = toneColour,
            maxLines = 1,
        )
    }
}

private fun toneColourFor(
    tone: BurpRemoteStatusTone,
    colourScheme: BurpRemoteColourScheme,
): Color =
    when (tone) {
        BurpRemoteStatusTone.Neutral -> colourScheme.contentSecondary
        BurpRemoteStatusTone.Live -> colourScheme.success
        BurpRemoteStatusTone.Warning -> colourScheme.warning
        BurpRemoteStatusTone.Danger -> colourScheme.danger
    }

private const val PILL_FILL_ALPHA = 0.16f
private const val PILL_BORDER_ALPHA = 0.4f
