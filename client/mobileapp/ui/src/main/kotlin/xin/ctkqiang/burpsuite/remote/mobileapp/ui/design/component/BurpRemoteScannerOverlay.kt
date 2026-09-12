package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.R
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens

/**
 * 扫码遮罩：四角括号、扫描线、状态说明与手电筒开关。
 *
 * 只画遮罩，不含相机：相机是平台权限与设备后端的事，遮罩只负责告诉用户「把码放进框里」。
 * 手电筒图样自己画——图标核心集里没有手电筒，为一个图标引入整套扩展集不值得。
 */
@Composable
fun BurpRemoteScannerOverlay(
    isTorchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    statusText: String,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val haptics = rememberBurpRemoteHaptics()
    val frameColour = tokens.colourScheme.accent
    val scanProgress by
        rememberInfiniteTransition(label = "scanner-line").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(BurpRemoteMotion.DURATION_EMPHASISED * 6, easing = BurpRemoteMotion.EasingStandard),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "scanner-line-progress",
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(tokens.colourScheme.scrim),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shortestSide = size.minDimension
            val inset = shortestSide * FRAME_INSET_RATIO
            val bracketLength = shortestSide * BRACKET_LENGTH_RATIO
            val strokePixels = FRAME_STROKE.toPx()
            val right = size.width - inset
            val bottom = size.height - inset

            // 左上
            drawLine(frameColour, Offset(inset, inset), Offset(inset + bracketLength, inset), strokePixels)
            drawLine(frameColour, Offset(inset, inset), Offset(inset, inset + bracketLength), strokePixels)
            // 右上
            drawLine(frameColour, Offset(right, inset), Offset(right - bracketLength, inset), strokePixels)
            drawLine(frameColour, Offset(right, inset), Offset(right, inset + bracketLength), strokePixels)
            // 左下
            drawLine(frameColour, Offset(inset, bottom), Offset(inset + bracketLength, bottom), strokePixels)
            drawLine(frameColour, Offset(inset, bottom), Offset(inset, bottom - bracketLength), strokePixels)
            // 右下
            drawLine(frameColour, Offset(right, bottom), Offset(right - bracketLength, bottom), strokePixels)
            drawLine(frameColour, Offset(right, bottom), Offset(right, bottom - bracketLength), strokePixels)

            val scanLineY = inset + (bottom - inset) * scanProgress
            drawLine(
                color = frameColour.copy(alpha = SCAN_LINE_ALPHA),
                start = Offset(inset, scanLineY),
                end = Offset(right, scanLineY),
                strokeWidth = strokePixels,
                cap = StrokeCap.Round,
            )
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = BurpRemoteSpacing.ExtraExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(BurpRemoteRadius.Capsule))
                        .background(tokens.colourScheme.surfaceElevated.copy(alpha = STATUS_BACKGROUND_ALPHA))
                        .padding(horizontal = BurpRemoteSpacing.Large, vertical = BurpRemoteSpacing.Small),
            ) {
                BurpRemoteText(
                    text = statusText,
                    style = tokens.typography.body,
                    colour = tokens.colourScheme.contentPrimary,
                )
            }

            Spacer(modifier = Modifier.height(BurpRemoteSpacing.ExtraLarge))

            Box(
                modifier =
                    Modifier
                        .size(TORCH_BUTTON_SIZE)
                        .clip(CircleShape)
                        .background(tokens.colourScheme.surfaceElevated.copy(alpha = STATUS_BACKGROUND_ALPHA))
                        .clickable {
                            haptics.select()
                            onToggleTorch()
                        },
                contentAlignment = Alignment.Center,
            ) {
                TorchGlyph(
                    isTorchEnabled = isTorchEnabled,
                    glyphColour = if (isTorchEnabled) frameColour else tokens.colourScheme.contentSecondary,
                    description = stringResource(R.string.components_scanner_toggle_torch),
                    modifier = Modifier.size(TORCH_GLYPH_SIZE),
                )
            }
        }
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
        modifier =
            modifier.semantics { contentDescription = description },
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

private val FRAME_STROKE = 3.dp
private val TORCH_BUTTON_SIZE = 52.dp
private val TORCH_GLYPH_SIZE = 24.dp
private const val FRAME_INSET_RATIO = 0.12f
private const val BRACKET_LENGTH_RATIO = 0.12f
private const val SCAN_LINE_ALPHA = 0.85f
private const val STATUS_BACKGROUND_ALPHA = 0.9f
private const val TORCH_BODY_WIDTH_RATIO = 0.44f
private const val TORCH_BODY_HEIGHT_RATIO = 0.42f
private const val TORCH_HEAD_WIDTH_RATIO = 1.5f
private const val TORCH_HEAD_HEIGHT_RATIO = 0.16f
private const val TORCH_CORNER_RATIO = 0.08f
private const val TORCH_BEAM_OFFSET_RATIO = 0.1f
private const val TORCH_BEAM_LENGTH_RATIO = 0.16f
private const val TORCH_BEAM_STROKE_RATIO = 0.08f
