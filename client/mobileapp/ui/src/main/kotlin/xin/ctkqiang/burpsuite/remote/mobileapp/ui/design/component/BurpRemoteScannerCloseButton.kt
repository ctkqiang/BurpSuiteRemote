package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 相机画面上的关闭入口：圆形半透明底 + 一枚叉。
 *
 * 取景是沉浸式语境，退出动作写成文字按钮就会与模式切换连成一条 Action Bar 的观感，
 * 因此这里只留一个圆：底用遮罩色、叉用压在遮罩上的字色，深浅两套主题下都只是取景框角上的一枚印记。
 *
 * @param contentDescription 无障碍描述；图标没有文字，缺了它读屏只能念出「按钮」。
 * @param onClick 点击后的行为。
 * @param modifier 由调用方决定摆放。
 */
@Composable
fun BurpRemoteScannerCloseButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()

    Box(
        modifier =
            modifier
                .size(BurpRemoteSizing.MinimumTouchTarget)
                .clip(CircleShape)
                .background(color = tokens.colourScheme.scrim, shape = CircleShape)
                .clickable {
                    haptics.tap()
                    onClick()
                },
        contentAlignment = Alignment.Center,
    ) {
        CloseGlyph(
            glyphColour = tokens.colourScheme.onScrim,
            description = contentDescription,
            modifier = Modifier.size(BurpRemoteSizing.Icon),
        )
    }
}

/** 叉由两笔斜线画成；端点收圆，因此放大后不会出现尖角。 */
@Composable
private fun CloseGlyph(
    glyphColour: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        drawLine(
            color = glyphColour,
            start = Offset(size.width * GLYPH_INSET_RATIO, size.height * GLYPH_INSET_RATIO),
            end = Offset(size.width * GLYPH_FAR_INSET_RATIO, size.height * GLYPH_FAR_INSET_RATIO),
            strokeWidth = size.width * GLYPH_STROKE_RATIO,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = glyphColour,
            start = Offset(size.width * GLYPH_FAR_INSET_RATIO, size.height * GLYPH_INSET_RATIO),
            end = Offset(size.width * GLYPH_INSET_RATIO, size.height * GLYPH_FAR_INSET_RATIO),
            strokeWidth = size.width * GLYPH_STROKE_RATIO,
            cap = StrokeCap.Round,
        )
    }
}

// 两笔斜线各留出四分之一边长的余量，圆底与叉之间才有呼吸。
private const val GLYPH_INSET_RATIO = 0.28f
private const val GLYPH_FAR_INSET_RATIO = 0.72f
private const val GLYPH_STROKE_RATIO = 0.1f
