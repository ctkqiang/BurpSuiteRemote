package xin.ctkqiang.burpsuite.remote.mobileapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 「尚未实现」的说明文案。
 *
 * 尚未实现的动作一律禁用并把原因写在旁边，而不是留一个点了没反应的按钮。
 */
@Composable
fun NotImplementedReasonText(
    @StringRes reasonResource: Int,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    BurpRemoteText(
        text = stringResource(R.string.components_not_implemented_reason, stringResource(reasonResource)),
        style = tokens.typography.label,
        colour = tokens.colourScheme.contentSecondary,
        modifier = modifier,
    )
}
