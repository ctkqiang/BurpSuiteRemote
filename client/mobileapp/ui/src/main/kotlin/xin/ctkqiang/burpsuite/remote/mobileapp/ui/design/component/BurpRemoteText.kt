package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * 界面里的统一文本渲染。
 *
 * 字号与字色都必须由调用方从主题取好，这里不写死任何一档：写死了就没人负责在深色下
 * 把字色换成对应的那一档（rules.md §3.3、§10）。
 *
 * 公开而不是模块内可见：各 feature 的自由排版（列表行、卡片内部）要渲染文本，
 * 藏起来只会让每个 feature 各抄一份同样的 `BasicText` 包装——那样抄出来的每份都会独立演化。
 *
 * @param text 要渲染的文本。
 * @param style 字号与行高等排版信息，取自主题字阶。
 * @param colour 字色，取自主题配色。
 * @param modifier 由调用方决定摆放。
 * @param maxLines 最多显示几行。
 * @param overflow 超出后的截断方式。
 * @param textAlign 水平对齐方式。
 */
@Composable
fun BurpRemoteText(
    text: String,
    style: TextStyle,
    colour: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign = TextAlign.Start,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = colour, textAlign = textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}
