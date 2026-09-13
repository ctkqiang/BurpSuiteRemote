// 请求的方法色标。

package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 方法色标：请求行左侧的一竖条。
 *
 * 同一类方法在每一屏都落在同一个颜色上，纵向扫一列时不必读动词就能看出哪几条会改状态。
 * 尺寸固定，不随字号变——它是列表的节奏线，不是文本的一部分。
 *
 * @param tone 该方法在界面上的语气，由 [burpRemoteMethodTone] 判定。
 * @param modifier 由调用方决定摆放。
 */
@Composable
fun BurpRemoteMethodMarker(
    tone: BurpRemoteStatusTone,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val shape = RoundedCornerShape(BurpRemoteRadius.Capsule)

    Box(
        modifier =
            modifier
                .width(MARKER_WIDTH)
                .height(MARKER_HEIGHT)
                .clip(shape)
                .background(color = tone.colourIn(tokens.colourScheme), shape = shape),
    )
}

private val MARKER_WIDTH = 4.dp
private val MARKER_HEIGHT = 20.dp
