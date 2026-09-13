// 代码块：等宽、语法着色、长按整段复制。

package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.burpRemoteLongPressClipboard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 代码块的语法档位。
 *
 * 只保留两档：真做着色就得对每种语言负责，声称支持却着错色比不着色更误导人。
 * 认不出的内容一律走 [PlainText]，等宽加整段复制已经足够完成比对。
 */
enum class BurpRemoteCodeLanguage {
    /** 不做语法着色；HTML、XML 与任何认不出的正文走这一档。 */
    PlainText,

    /** JSON。容忍 `//` 与 `/* */` 注释——插件抓到的配置片段常常带着注释。 */
    Json,
}

/**
 * 代码块：整块等宽、按语言着色、长按把原文复制走。
 *
 * 复制的是未经着色的原文而不是屏幕上的分段文本：赏金猎人要把它贴进报告或发回给插件，
 * 带上一堆空样式片段只会碍事。
 *
 * @param text 要展示的原文。
 * @param modifier 由调用方决定摆放。
 * @param language 语法档位；默认不着色。
 */
@Composable
fun BurpRemoteCodeBlock(
    text: String,
    modifier: Modifier = Modifier,
    language: BurpRemoteCodeLanguage = BurpRemoteCodeLanguage.PlainText,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val colourScheme = tokens.colourScheme
    val shape = RoundedCornerShape(BurpRemoteRadius.Card)
    val highlighted =
        remember(text, language, colourScheme) {
            highlightedCode(text = text, language = language, colourScheme = colourScheme)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(color = colourScheme.surface, shape = shape)
                .border(width = 1.dp, color = colourScheme.outline, shape = shape)
                .burpRemoteLongPressClipboard(text)
                .padding(BurpRemoteSpacing.Medium),
    ) {
        BasicText(text = highlighted, style = tokens.typography.technical)
    }
}

/**
 * 把原文切成带样式的串。
 *
 * @param text 原文。
 * @param language 语法档位。
 * @param colourScheme 当前主题的配色；着色用的色号全部来自它（rules.md §10）。
 * @return 可直接交给文本控件渲染的带样式串。
 */
private fun highlightedCode(
    text: String,
    language: BurpRemoteCodeLanguage,
    colourScheme: BurpRemoteColourScheme,
): AnnotatedString {
    val segments =
        when (language) {
            BurpRemoteCodeLanguage.PlainText -> listOf(CodeSegment(text, CodeSegmentKind.PlainText))
            BurpRemoteCodeLanguage.Json -> jsonSegments(text)
        }

    return buildAnnotatedString {
        for (segment in segments) {
            withStyle(SpanStyle(color = segment.kind.colourIn(colourScheme))) { append(segment.text) }
        }
    }
}

/** 原文里的一段，以及它该被当成什么来着色。 */
private data class CodeSegment(
    val text: String,
    val kind: CodeSegmentKind,
)

/** 着色分档；分档越少越不容易着错。 */
private enum class CodeSegmentKind {
    PropertyKey,
    StringLiteral,
    NumberLiteral,
    KeywordLiteral,
    Comment,
    Punctuation,
    PlainText,
}

/**
 * 把一段 JSON 扫成带分档的段。
 *
 * 单趟从左到右扫描，不做回溯、也不校验合法性：这里是着色器不是解析器，遇到畸形内容
 * 只需要保证「不丢字符、不倒序」，颜色分错无所谓——真正要判合法性的地方在协议层。
 *
 * @param source 原文。
 * @return 按原顺序排列的分段，拼起来与原文逐字符相同。
 */
private fun jsonSegments(source: String): List<CodeSegment> {
    val segments = mutableListOf<CodeSegment>()
    var index = 0

    while (index < source.length) {
        val character = source[index]
        when {
            character.isWhitespace() -> {
                val start = index
                while (index < source.length && source[index].isWhitespace()) index++
                segments += CodeSegment(source.substring(start, index), CodeSegmentKind.PlainText)
            }

            character == QUOTE -> {
                val end = quotedTextEnd(source = source, index = index)
                val kind =
                    if (isPropertyKey(source = source, index = end)) {
                        CodeSegmentKind.PropertyKey
                    } else {
                        CodeSegmentKind.StringLiteral
                    }
                segments += CodeSegment(source.substring(index, end), kind)
                index = end
            }

            character == MINUS || character.isDigit() -> {
                val start = index
                index++
                while (index < source.length && source[index].isNumberTail()) index++
                segments += CodeSegment(source.substring(start, index), CodeSegmentKind.NumberLiteral)
            }

            else -> index = appendBareSegment(source = source, index = index, segments = segments)
        }
    }

    return segments
}

/**
 * 处理不属于空白的非字面量字符：注释、关键字字面量、结构符号，都不属于时按普通文本收下。
 *
 * @param source 原文。
 * @param index 当前下标。
 * @param segments 已收集的分段，函数会往里追加。
 * @return 下一个待处理的下标。
 */
private fun appendBareSegment(
    source: String,
    index: Int,
    segments: MutableList<CodeSegment>,
): Int {
    val commentEnd = commentEnd(source, index)
    if (commentEnd != null) {
        segments += CodeSegment(source.substring(index, commentEnd), CodeSegmentKind.Comment)
        return commentEnd
    }

    val keyword = KEYWORD_LITERALS.firstOrNull { literal -> source.startsWith(literal, index) }
    if (keyword != null) {
        segments += CodeSegment(keyword, CodeSegmentKind.KeywordLiteral)
        return index + keyword.length
    }

    val character = source[index]
    val kind = if (character in PUNCTUATION_CHARACTERS) CodeSegmentKind.Punctuation else CodeSegmentKind.PlainText
    segments += CodeSegment(character.toString(), kind)
    return index + 1
}

/**
 * 从开引号处起，找闭合引号之后的下标。
 *
 * 反斜杠转义按两个字符一起跳过，否则 `"a\"b"` 会被当成在 `\"` 处收尾。
 *
 * @param source 原文。
 * @param index 开引号的下标。
 * @return 闭合引号之后的下标；没有闭合时返回串尾。
 */
private fun quotedTextEnd(
    source: String,
    index: Int,
): Int {
    var cursor = index + 1
    while (cursor < source.length) {
        when (source[cursor]) {
            BACKSLASH -> cursor += 2
            QUOTE -> return cursor + 1
            else -> cursor++
        }
    }
    return source.length
}

/**
 * 判断一个引号串是不是键名。
 *
 * @param source 原文。
 * @param index 闭合引号之后的下标。
 * @return 其后（跳过空白）紧跟冒号时为 `true`。
 */
private fun isPropertyKey(
    source: String,
    index: Int,
): Boolean {
    var cursor = index
    while (cursor < source.length && source[cursor].isWhitespace()) cursor++
    return cursor < source.length && source[cursor] == COLON
}

/**
 * 从某处起是否是一段注释，以及它在哪里结束。
 *
 * @param source 原文。
 * @param index 当前下标。
 * @return 注释结束后的下标；此处不是注释时返回 `null`。
 */
private fun commentEnd(
    source: String,
    index: Int,
): Int? {
    if (source.startsWith(LINE_COMMENT_PREFIX, index)) {
        val lineEnd = source.indexOf('\n', index)
        return if (lineEnd == -1) source.length else lineEnd
    }
    if (source.startsWith(BLOCK_COMMENT_PREFIX, index)) {
        val blockEnd = source.indexOf(BLOCK_COMMENT_SUFFIX, index + BLOCK_COMMENT_PREFIX.length)
        return if (blockEnd == -1) source.length else blockEnd + BLOCK_COMMENT_SUFFIX.length
    }
    return null
}

/** 数值是否还能往下延伸；指数与正负号只有紧跟数字时才有意义，这里宽松处理也不影响着色。 */
private fun Char.isNumberTail(): Boolean = isDigit() || this in NUMBER_TAIL_CHARACTERS

/** 分档到颜色：着色用的每一档都在主题里有对应槽位。 */
private fun CodeSegmentKind.colourIn(colourScheme: BurpRemoteColourScheme): Color =
    when (this) {
        CodeSegmentKind.PropertyKey -> colourScheme.codeKey
        CodeSegmentKind.StringLiteral -> colourScheme.codeString
        CodeSegmentKind.NumberLiteral -> colourScheme.codeNumber
        CodeSegmentKind.KeywordLiteral -> colourScheme.codeLiteral
        CodeSegmentKind.Comment -> colourScheme.codeComment
        CodeSegmentKind.Punctuation -> colourScheme.contentSecondary
        CodeSegmentKind.PlainText -> colourScheme.contentPrimary
    }

private const val QUOTE = '"'
private const val COLON = ':'
private const val MINUS = '-'
private const val BACKSLASH = '\\'
private const val LINE_COMMENT_PREFIX = "//"
private const val BLOCK_COMMENT_PREFIX = "/*"
private const val BLOCK_COMMENT_SUFFIX = "*/"

private val KEYWORD_LITERALS = listOf("true", "false", "null")
private val PUNCTUATION_CHARACTERS = setOf('{', '}', '[', ']', ':', ',')
private val NUMBER_TAIL_CHARACTERS = setOf('.', 'e', 'E', '+', '-')
