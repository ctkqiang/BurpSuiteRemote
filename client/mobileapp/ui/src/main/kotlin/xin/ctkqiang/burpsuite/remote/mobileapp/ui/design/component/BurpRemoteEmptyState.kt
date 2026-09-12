package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 空态。
 *
 * 空列表必须回答「为什么空、下一步做什么」：只写一句「暂无数据」，用户会以为是自己把数据弄丢了。
 */
@Composable
fun BurpRemoteEmptyState(
    headline: String,
    detail: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BurpRemoteSpacing.ExtraLarge,
                    vertical = BurpRemoteSpacing.ExtraExtraLarge,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BurpRemoteText(
            text = headline,
            style = tokens.typography.title,
            colour = tokens.colourScheme.contentPrimary,
        )
        Spacer(modifier = Modifier.height(BurpRemoteSpacing.Small))
        BurpRemoteText(
            text = detail,
            style = tokens.typography.body,
            colour = tokens.colourScheme.contentSecondary,
        )
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(BurpRemoteSpacing.Large))
            BurpRemoteButton(
                text = actionText,
                onClick = onAction,
                style = BurpRemoteButtonStyle.Secondary,
            )
        }
    }
}
