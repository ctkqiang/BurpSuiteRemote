package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 一组列表行共用的容器。
 *
 * 一整组共用一个描边与一层底色，因此组内几行读起来是「同一张卡里的几行」，而不是几张各自独立的卡片。
 * 内边距不在这里给：每一行自己知道左右该留多少，重复给一次会让行内文字被推两次。
 *
 * @param content 组内的行；行的 [BurpRemoteListItem.showsDivider] 由调用方按位置决定。
 */
@Composable
fun BurpRemoteListItemGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val shape = RoundedCornerShape(BurpRemoteRadius.Card)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(color = tokens.colourScheme.surface, shape = shape)
                .border(width = 1.dp, color = tokens.colourScheme.outline, shape = shape),
        content = content,
    )
}
