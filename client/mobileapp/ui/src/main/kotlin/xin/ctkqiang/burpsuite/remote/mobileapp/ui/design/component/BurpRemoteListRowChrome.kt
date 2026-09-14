package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 懒加载列表里的一行所带的卡片外框。
 *
 * [BurpRemoteListItemGroup] 把一整组行放进同一张卡，一屏记录因此读成一条时间线；但那份容器是一个
 * 整体，要求组内所有行在同一帧里组合出来。记录一多，首帧就要把每一条都量一遍，掉帧就出在这里。
 *
 * 于是外框改由每一行自己带：首行画上边与上圆角，末行画下边与下圆角，中间的行只画左右两侧。
 * 上下两边只在组的端点出现，所以行与行之间仍然只有一条线——那条线由
 * [BurpRemoteListItem] 的 `showsDivider` 画，这里绝不重复画上边，否则会出现一实一虚两条并排。
 *
 * 行的左右两侧始终画满整行高度，相邻两行的侧边因此首尾相接，看起来还是一张连续的卡。
 *
 * 用法：只给懒加载列表里的行加这个外框，且同一个列表里每行都要加；
 * 整组渲染的场合仍然用 [BurpRemoteListItemGroup]。
 *
 * @param isFirstRow 这一行是不是这一组的首行；决定上边与上圆角画不画。
 * @param isLastRow 这一行是不是这一组的末行；决定下边与下圆角画不画。
 */
@Composable
fun Modifier.burpRemoteListRowChrome(
    isFirstRow: Boolean,
    isLastRow: Boolean,
): Modifier {
    val tokens = LocalBurpRemoteDesignTokens.current
    val shape =
        RoundedCornerShape(
            topStart = if (isFirstRow) BurpRemoteRadius.Card else NO_CORNER_RADIUS,
            topEnd = if (isFirstRow) BurpRemoteRadius.Card else NO_CORNER_RADIUS,
            bottomStart = if (isLastRow) BurpRemoteRadius.Card else NO_CORNER_RADIUS,
            bottomEnd = if (isLastRow) BurpRemoteRadius.Card else NO_CORNER_RADIUS,
        )
    val outlineColour = tokens.colourScheme.outline

    return this
        .clip(shape)
        .background(color = tokens.colourScheme.surface, shape = shape)
        .drawBehind {
            drawRowSides(outlineColour = outlineColour, isFirstRow = isFirstRow, isLastRow = isLastRow)
            if (isFirstRow) drawRowTopEdge(outlineColour = outlineColour)
            if (isLastRow) drawRowBottomEdge(outlineColour = outlineColour)
        }
}

// 左右两侧：圆角行从圆角终点起画，免得直线压进圆弧里。
private fun DrawScope.drawRowSides(
    outlineColour: Color,
    isFirstRow: Boolean,
    isLastRow: Boolean,
) {
    val strokePx = OUTLINE_WIDTH.toPx()
    val halfStroke = strokePx / HALF
    val cornerRadius = cornerRadiusOf()

    val topStart = if (isFirstRow) cornerRadius else ZERO_INSET
    val bottomEnd = size.height - if (isLastRow) cornerRadius else ZERO_INSET

    drawLine(outlineColour, Offset(halfStroke, topStart), Offset(halfStroke, bottomEnd), strokePx)
    drawLine(
        outlineColour,
        Offset(size.width - halfStroke, topStart),
        Offset(size.width - halfStroke, bottomEnd),
        strokePx,
    )
}

// 上边：中间一条直线，两端各接一段四分之一圆弧，圆弧半径收在行高与行宽之内。
private fun DrawScope.drawRowTopEdge(outlineColour: Color) {
    val strokePx = OUTLINE_WIDTH.toPx()
    val halfStroke = strokePx / HALF
    val cornerRadius = cornerRadiusOf()
    val arcSize = Size(cornerRadius * TWO - strokePx, cornerRadius * TWO - strokePx)

    drawLine(outlineColour, Offset(cornerRadius, halfStroke), Offset(size.width - cornerRadius, halfStroke), strokePx)
    drawArc(
        color = outlineColour,
        startAngle = START_ANGLE_TOP_LEFT,
        sweepAngle = QUARTER_TURN_ANGLE,
        useCenter = false,
        topLeft = Offset(halfStroke, halfStroke),
        size = arcSize,
        style = Stroke(width = strokePx),
    )
    drawArc(
        color = outlineColour,
        startAngle = START_ANGLE_TOP_RIGHT,
        sweepAngle = QUARTER_TURN_ANGLE,
        useCenter = false,
        topLeft = Offset(size.width - TWO * cornerRadius + halfStroke, halfStroke),
        size = arcSize,
        style = Stroke(width = strokePx),
    )
}

// 下边与上边对称，只是圆弧的起点角度换成下半圈的那两个。
private fun DrawScope.drawRowBottomEdge(outlineColour: Color) {
    val strokePx = OUTLINE_WIDTH.toPx()
    val halfStroke = strokePx / HALF
    val cornerRadius = cornerRadiusOf()
    val arcSize = Size(cornerRadius * TWO - strokePx, cornerRadius * TWO - strokePx)
    val baseline = size.height - halfStroke
    val arcTop = size.height - TWO * cornerRadius + halfStroke

    drawLine(outlineColour, Offset(cornerRadius, baseline), Offset(size.width - cornerRadius, baseline), strokePx)
    drawArc(
        color = outlineColour,
        startAngle = START_ANGLE_BOTTOM_RIGHT,
        sweepAngle = QUARTER_TURN_ANGLE,
        useCenter = false,
        topLeft = Offset(size.width - TWO * cornerRadius + halfStroke, arcTop),
        size = arcSize,
        style = Stroke(width = strokePx),
    )
    drawArc(
        color = outlineColour,
        startAngle = START_ANGLE_BOTTOM_LEFT,
        sweepAngle = QUARTER_TURN_ANGLE,
        useCenter = false,
        topLeft = Offset(halfStroke, arcTop),
        size = arcSize,
        style = Stroke(width = strokePx),
    )
}

// 行矮到放不下圆角时按半高收，否则圆弧会翻出去变成一段突兀的弧。
private fun DrawScope.cornerRadiusOf(): Float =
    minOf(BurpRemoteRadius.Card.toPx(), minOf(size.width, size.height) / HALF)

private val NO_CORNER_RADIUS = 0.dp
private val OUTLINE_WIDTH = 1.dp

private const val HALF = 2f
private const val TWO = 2f
private const val ZERO_INSET = 0f

// 角度以三点钟方向为 0°、顺时针为正：左上角从 180° 起、右上角从 270° 起，
// 下半圈同理换成 90°（左下）与 0°（右下）。
private const val QUARTER_TURN_ANGLE = 90f
private const val START_ANGLE_TOP_LEFT = 180f
private const val START_ANGLE_TOP_RIGHT = 270f
private const val START_ANGLE_BOTTOM_RIGHT = 0f
private const val START_ANGLE_BOTTOM_LEFT = 90f
