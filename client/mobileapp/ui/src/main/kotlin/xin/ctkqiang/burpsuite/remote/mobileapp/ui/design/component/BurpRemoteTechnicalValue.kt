package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.burpRemoteLongPressClipboard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 技术值：等宽显示，长按整段复制。
 *
 * 赏金猎人要把主机、路径、状态码逐字符比对，也得整段贴进报告；比例字体和「选中一半」都做不到这件事。
 */
@Composable
fun BurpRemoteTechnicalValue(
    text: String,
    label: String? = null,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .burpRemoteLongPressClipboard(text)
                .padding(vertical = BurpRemoteSpacing.ExtraSmall),
    ) {
        if (label != null) {
            BurpRemoteText(
                text = label,
                style = tokens.typography.label,
                colour = tokens.colourScheme.contentSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BurpRemoteText(
            text = text,
            style = tokens.typography.technical,
            colour = tokens.colourScheme.contentPrimary,
        )
    }
}
