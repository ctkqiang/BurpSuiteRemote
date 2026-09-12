package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 段落标题。
 *
 * [trailing] 放计数、筛选这类与标题同行的动作；需要解释标题的说明放在标题下方，不要挤进这一行。
 */
@Composable
fun BurpRemoteSectionHeading(
    text: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BurpRemoteSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            BurpRemoteText(
                text = text,
                style = tokens.typography.title,
                colour = tokens.colourScheme.contentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}
