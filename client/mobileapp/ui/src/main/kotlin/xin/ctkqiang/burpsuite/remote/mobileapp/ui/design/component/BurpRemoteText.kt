package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

// 组件内部的统一文本渲染：字号与字色都由调用方从主题取好，这里不写死任何一档。
@Composable
internal fun BurpRemoteText(
    text: String,
    style: TextStyle,
    colour: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = colour),
        maxLines = maxLines,
        overflow = overflow,
    )
}
