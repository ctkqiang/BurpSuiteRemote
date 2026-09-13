package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 错误态：危险色图标 + 可读原因 + 重试按钮。
 *
 * 失败必须写成「发生了什么」，而不是「出错了」：用户据此才知道重试有没有意义。
 * 图标与空态共用同一档 32dp，一列里两种状态因此不会一高一矮。
 */
@Composable
fun BurpRemoteErrorState(
    detail: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BurpRemoteSpacing.ExtraLarge,
                    vertical = BurpRemoteSpacing.ExtraExtraLarge,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = rememberVectorPainter(Icons.Filled.Warning),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tokens.colourScheme.danger),
            modifier = Modifier.size(BurpRemoteSizing.StateIcon),
        )
        Spacer(modifier = Modifier.height(BurpRemoteSpacing.Large))
        BurpRemoteText(
            text = detail,
            style = tokens.typography.label,
            colour = tokens.colourScheme.contentPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(BurpRemoteSpacing.Large))
        BurpRemoteButton(
            text = stringResource(R.string.components_retry),
            onClick = onRetry,
            style = BurpRemoteButtonStyle.Secondary,
        )
    }
}
