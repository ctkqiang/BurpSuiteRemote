package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 列表行：标题、可选说明、左右两个插槽，右侧可挂一枚「进入下一级」的箭头。
 *
 * 行的底色与描边通常由外面那层容器（[BurpRemoteListItemGroup]）给，它自己只画内容——这样一整组行看起来
 * 是一张卡里的几行，而不是几张各自独立的卡片叠在一起。惰性列表里没法用那份容器（一个容器要求在
 * 同一帧里组合出组内所有行），改由每一行自带外框，见 [burpRemoteListRowChrome]。
 *
 * [showsDivider] 由调用方按位置决定：只有调用方知道谁是这一组的最后一行，而最后一行下面那条线
 * 会与容器的下边框贴成两条，看起来像画歪了。
 *
 * [modifier] 加在最外层，管住「这一行 + 它下面那条分隔线」：行外框的左右边线要一直画到分隔线底部，
 * 位移与淡入也该连分隔线一起走。
 *
 * @param title 这一行做什么。
 * @param subtitle 补一句它影响什么；不需要解释时留空，不要为了填满而写废话。
 * @param titleIsTechnical 标题是否是技术值（请求行、标识符这类）。技术值走等宽字阶，
 *   多个行纵向对齐时能逐字符比对——这是「读请求」和「读句子」的区别。
 * @param leading 标题左边的插槽，例如方法色标。
 * @param trailing 右侧插槽，例如状态胶囊。
 * @param showsChevron 这一行点进去是下一级时置真；只切换开关、不跳转的行不要挂箭头。
 * @param showsDivider 这一行下面是否画分隔线；一组的最后一行传假。
 * @param onClick 整行的点击行为；为空时这一行不可点，也不会有触感反馈。
 */
@Composable
fun BurpRemoteListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleIsTechnical: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showsChevron: Boolean = false,
    showsDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // 一行被按下时轻微内缩而不是整块变色：列表里一次只会动一行，扫读时不会被大色块打断。
    val pressScale by
        animateFloatAsState(
            targetValue = if (isPressed) PRESSED_SCALE else RESTING_SCALE,
            animationSpec = tween(BurpRemoteMotion.DURATION_FAST, easing = BurpRemoteMotion.EasingStandard),
            label = "list-item-press-scale",
        )

    // 调用方的 modifier 加在最外层，管住「这一行 + 它下面那条分隔线」：外框的左右两条边线要一直画到
    // 分隔线底部，位移与淡入也该连分隔线一起走；只加在行上，边线会在每条分隔线那里断掉一档。
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {
                                haptics.tap()
                                onClick()
                            }
                        } else {
                            Modifier
                        },
                    )
                    .defaultMinSize(minHeight = BurpRemoteSizing.MinimumTouchTarget)
                    .padding(
                        horizontal = BurpRemoteSpacing.Large,
                        vertical = BurpRemoteSpacing.Medium,
                    ),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
            }
            Column(
                modifier = Modifier.weight(weight = 1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraSmall),
            ) {
                BurpRemoteText(
                    text = title,
                    style = if (titleIsTechnical) tokens.typography.technical else tokens.typography.body,
                    colour = tokens.colourScheme.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    BurpRemoteText(
                        text = subtitle,
                        style = tokens.typography.caption,
                        colour = tokens.colourScheme.contentSecondary,
                        maxLines = SUBTITLE_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            }
            if (showsChevron) {
                ChevronGlyph(modifier = Modifier.size(BurpRemoteSizing.InlineIcon))
            }
        }

        if (showsDivider) {
            ListDivider()
        }
    }
}

/**
 * 行与行之间的分隔线。
 *
 * 只画一条 1dp 细线，不画阴影也不画第二种底色：一组成员之间的关系靠「同在一个描边容器里」表达，
 * 再叠一层底色只会把它画成两张卡。左右各留出与行内文字同样的内边距，线因此不会顶到容器边框上。
 */
@Composable
private fun ListDivider() {
    val tokens = LocalBurpRemoteDesignTokens.current

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = BurpRemoteSpacing.Large, end = BurpRemoteSpacing.Large)
                .height(BurpRemoteSizing.Divider),
    ) {
        val strokeWidth = size.height
        drawLine(
            color = tokens.colourScheme.outline,
            start = Offset(0f, strokeWidth / 2f),
            end = Offset(size.width, strokeWidth / 2f),
            strokeWidth = strokeWidth,
        )
    }
}

/**
 * 核心图标集里没有「下一级」的箭头，自己画一条。
 *
 * 与设计系统里扫码图标的做法一致：不为一枚箭头引入整套扩展图标集，那会让包体为一个 20dp 的图形涨一截。
 */
@Composable
private fun ChevronGlyph(modifier: Modifier = Modifier) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val colour = tokens.colourScheme.contentSecondary

    Canvas(modifier = modifier) {
        val strokeWidth = size.height * CHEVRON_STROKE_RATIO
        val startX = size.width * CHEVRON_START_RATIO
        val middleX = size.width * CHEVRON_MIDDLE_RATIO
        val middleY = size.height / 2f
        drawLine(
            color = colour,
            start = Offset(startX, size.height * CHEVRON_TOP_RATIO),
            end = Offset(middleX, middleY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colour,
            start = Offset(middleX, middleY),
            end = Offset(startX, size.height * CHEVRON_BOTTOM_RATIO),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

// 说明最多两行：再长就不该塞进列表行，那说明这一行需要的是自己的屏。
private const val SUBTITLE_MAX_LINES = 2

private const val PRESSED_SCALE = 0.99f
private const val RESTING_SCALE = 1f

private const val CHEVRON_STROKE_RATIO = 0.12f
private const val CHEVRON_START_RATIO = 0.3f
private const val CHEVRON_MIDDLE_RATIO = 0.7f
private const val CHEVRON_TOP_RATIO = 0.2f
private const val CHEVRON_BOTTOM_RATIO = 0.8f
