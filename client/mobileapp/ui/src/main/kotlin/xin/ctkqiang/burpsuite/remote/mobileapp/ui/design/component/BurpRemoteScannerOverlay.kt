package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 扫码遮罩：方框外压暗、四角括号、渐变扫描线、提示文案与手电筒开关。
 *
 * 只画遮罩，不含相机：相机是平台权限与设备后端的事，遮罩只负责告诉用户「把码放进框里」。
 * 方框是真的挖出来的（Clear 混合），不是盖一层浅色——压暗必须能透出相机画面，否则暗处无补光时
 * 用户看不到自己有没有对准。
 *
 * 提示文案的位置由同一组几何算出，因此它永远贴在方框下沿，不会因为字号或语言长短而跑位。
 * 手电筒图样自己画——图标核心集里没有手电筒，为一个图标引入整套扩展集不值得。
 *
 * 压暗铺满整屏（状态栏与手势条那两条也要盖住），但那只是背景；取景方框、四角括号与扫描线的几何
 * 一律按安全区算，刘海、挖孔与系统栏都不参与定位，因此在挖孔机型上括号不会被裁掉。
 */
@Composable
fun BurpRemoteScannerOverlay(
    isTorchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    statusText: String,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()
    val layoutDirection = LocalLayoutDirection.current
    val safeAreaPadding =
        WindowInsets.systemBars
            .union(WindowInsets.displayCutout)
            .asPaddingValues()
    val scanProgress by
        rememberInfiniteTransition(label = "scanner-line").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(SCAN_LINE_DURATION, easing = BurpRemoteMotion.EasingStandard),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "scanner-line-progress",
        )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val safeAreaLeft = safeAreaPadding.calculateLeftPadding(layoutDirection)
        val safeAreaTop = safeAreaPadding.calculateTopPadding()
        val safeAreaRight = safeAreaPadding.calculateRightPadding(layoutDirection)
        val safeAreaBottom = safeAreaPadding.calculateBottomPadding()
        val safeAreaWidth = maxWidth - safeAreaLeft - safeAreaRight
        val safeAreaHeight = maxHeight - safeAreaTop - safeAreaBottom
        val frameSide = minOf(safeAreaWidth, safeAreaHeight) * FRAME_SIDE_RATIO
        val frameLeft = safeAreaLeft + (safeAreaWidth - frameSide) / 2
        val frameTop = safeAreaTop + (safeAreaHeight - frameSide) / 2

        ScannerMask(
            scrimColour = tokens.colourScheme.scrim,
            accentColour = tokens.colourScheme.accent,
            frameLeft = frameLeft,
            frameTop = frameTop,
            frameSide = frameSide,
            scanProgress = scanProgress,
        )

        BurpRemoteText(
            text = statusText,
            style = tokens.typography.subtitle,
            colour = tokens.colourScheme.onScrim,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        start = safeAreaLeft + BurpRemoteSpacing.ScreenEdge,
                        end = safeAreaRight + BurpRemoteSpacing.ScreenEdge,
                        top = frameTop + frameSide + BurpRemoteSpacing.ScreenEdge,
                    ),
            textAlign = TextAlign.Center,
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = BurpRemoteSpacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TorchAction(
                isTorchEnabled = isTorchEnabled,
                glyphColour =
                    if (isTorchEnabled) tokens.colourScheme.accent else tokens.colourScheme.onScrim,
                containerColour = tokens.colourScheme.onScrim.copy(alpha = TORCH_BACKGROUND_ALPHA),
                description = stringResource(R.string.components_scanner_toggle_torch),
                onClick = {
                    haptics.select()
                    onToggleTorch()
                },
            )
        }
    }
}

// 一层画完遮罩、挖孔、括号与扫描线：分成多层会让 Clear 把下面那层的括号也一起挖掉。
@Composable
private fun ScannerMask(
    scrimColour: Color,
    accentColour: Color,
    frameLeft: Dp,
    frameTop: Dp,
    frameSide: Dp,
    scanProgress: Float,
) {
    Canvas(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        drawRect(color = scrimColour)

        val left = frameLeft.toPx()
        val top = frameTop.toPx()
        val side = frameSide.toPx()
        val cornerPixels = FRAME_CORNER.toPx()
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(side, side),
            cornerRadius = CornerRadius(cornerPixels, cornerPixels),
            blendMode = BlendMode.Clear,
        )

        val strokePixels = FRAME_STROKE.toPx()
        val armPixels = side * BRACKET_ARM_RATIO
        val bracketLeft = left + side * BRACKET_INSET_RATIO
        val bracketTop = top + side * BRACKET_INSET_RATIO
        val bracketRight = left + side - side * BRACKET_INSET_RATIO
        val bracketBottom = top + side - side * BRACKET_INSET_RATIO

        drawCornerBrackets(
            accentColour = accentColour,
            strokePixels = strokePixels,
            armPixels = armPixels,
            left = bracketLeft,
            top = bracketTop,
            right = bracketRight,
            bottom = bracketBottom,
        )

        // 渐变扫描线：两端收回透明，来回一趟，因此不会在方框边缘留下一个硬端点。
        val scanLineY = top + side * scanProgress
        drawLine(
            brush =
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, accentColour, Color.Transparent),
                    startX = left,
                    endX = left + side,
                ),
            start = Offset(left, scanLineY),
            end = Offset(left + side, scanLineY),
            strokeWidth = SCAN_LINE_STROKE.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawCornerBrackets(
    accentColour: Color,
    strokePixels: Float,
    armPixels: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
) {
    drawLine(accentColour, Offset(left, top + armPixels), Offset(left, top), strokePixels, StrokeCap.Round)
    drawLine(accentColour, Offset(left, top), Offset(left + armPixels, top), strokePixels, StrokeCap.Round)
    drawLine(accentColour, Offset(right - armPixels, top), Offset(right, top), strokePixels, StrokeCap.Round)
    drawLine(accentColour, Offset(right, top), Offset(right, top + armPixels), strokePixels, StrokeCap.Round)
    drawLine(accentColour, Offset(left, bottom - armPixels), Offset(left, bottom), strokePixels, StrokeCap.Round)
    drawLine(accentColour, Offset(left, bottom), Offset(left + armPixels, bottom), strokePixels, StrokeCap.Round)
    drawLine(accentColour, Offset(right - armPixels, bottom), Offset(right, bottom), strokePixels, StrokeCap.Round)
    drawLine(accentColour, Offset(right, bottom), Offset(right, bottom - armPixels), strokePixels, StrokeCap.Round)
}

// 触控区 48dp、图样 24dp：圆形底色只是让它在相机画面上有个落脚点。
@Composable
private fun TorchAction(
    isTorchEnabled: Boolean,
    glyphColour: Color,
    containerColour: Color,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(BurpRemoteSizing.MinimumTouchTarget)
                .clip(CircleShape)
                .background(containerColour)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TorchGlyph(
            isTorchEnabled = isTorchEnabled,
            glyphColour = glyphColour,
            description = description,
            modifier = Modifier.size(BurpRemoteSizing.Icon),
        )
    }
}

@Composable
private fun TorchGlyph(
    isTorchEnabled: Boolean,
    glyphColour: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.semantics { contentDescription = description },
    ) {
        val bodyWidth = size.width * TORCH_BODY_WIDTH_RATIO
        val bodyHeight = size.height * TORCH_BODY_HEIGHT_RATIO
        val bodyLeft = (size.width - bodyWidth) / 2f
        val bodyTop = size.height - bodyHeight
        val headWidth = bodyWidth * TORCH_HEAD_WIDTH_RATIO
        val headHeight = size.height * TORCH_HEAD_HEIGHT_RATIO
        val cornerRadius = CornerRadius(size.width * TORCH_CORNER_RATIO, size.width * TORCH_CORNER_RATIO)

        drawRoundRect(
            color = glyphColour,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = glyphColour,
            topLeft = Offset((size.width - headWidth) / 2f, bodyTop - headHeight),
            size = Size(headWidth, headHeight),
            cornerRadius = cornerRadius,
        )
        if (isTorchEnabled) {
            val beamTop = bodyTop - headHeight - size.height * TORCH_BEAM_OFFSET_RATIO
            drawLine(
                color = glyphColour,
                start = Offset(size.width / 2f, beamTop),
                end = Offset(size.width / 2f, beamTop - size.height * TORCH_BEAM_LENGTH_RATIO),
                strokeWidth = size.width * TORCH_BEAM_STROKE_RATIO,
                cap = StrokeCap.Round,
            )
        }
    }
}

private val FRAME_CORNER = BurpRemoteRadius.Viewfinder
private val FRAME_STROKE = 3.dp
private val SCAN_LINE_STROKE = 2.dp
private const val FRAME_SIDE_RATIO = 0.62f
private const val BRACKET_INSET_RATIO = 0.06f
private const val BRACKET_ARM_RATIO = 0.14f
private const val SCAN_LINE_DURATION = 1400
private const val TORCH_BACKGROUND_ALPHA = 0.12f
private const val TORCH_BODY_WIDTH_RATIO = 0.44f
private const val TORCH_BODY_HEIGHT_RATIO = 0.42f
private const val TORCH_HEAD_WIDTH_RATIO = 1.5f
private const val TORCH_HEAD_HEIGHT_RATIO = 0.16f
private const val TORCH_CORNER_RATIO = 0.08f
private const val TORCH_BEAM_OFFSET_RATIO = 0.1f
private const val TORCH_BEAM_LENGTH_RATIO = 0.16f
private const val TORCH_BEAM_STROKE_RATIO = 0.08f
