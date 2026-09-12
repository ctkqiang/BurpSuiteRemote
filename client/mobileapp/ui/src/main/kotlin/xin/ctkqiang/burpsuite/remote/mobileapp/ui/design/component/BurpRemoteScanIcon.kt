package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 扫码动作的图标：四角取景框加一条横扫线。
 *
 * 自己画而不是取现成图标集：Material 核心图标集里没有扫码这一枚，而为它引入扩展集等于把整包
 * 图标塞进 APK。描边色是占位，使用方一律用 ColorFilter 按主题着色（rules.md §8.3）。
 */
val BurpRemoteScanIcon: ImageVector by lazy { buildScanIcon() }

private fun buildScanIcon(): ImageVector =
    ImageVector.Builder(
        name = "BurpRemoteScanIcon",
        defaultWidth = ICON_SIZE,
        defaultHeight = ICON_SIZE,
        viewportWidth = VIEWPORT_SIZE,
        viewportHeight = VIEWPORT_SIZE,
    ).path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        // 左上角括号
        moveTo(FRAME_INSET, CORNER_ARM_END)
        lineTo(FRAME_INSET, FRAME_INSET)
        lineTo(CORNER_ARM_END, FRAME_INSET)
        // 右上角括号
        moveTo(CORNER_ARM_START, FRAME_INSET)
        lineTo(FRAME_FAR_INSET, FRAME_INSET)
        lineTo(FRAME_FAR_INSET, CORNER_ARM_END)
        // 左下角括号
        moveTo(FRAME_INSET, CORNER_ARM_START)
        lineTo(FRAME_INSET, FRAME_FAR_INSET)
        lineTo(CORNER_ARM_END, FRAME_FAR_INSET)
        // 右下角括号
        moveTo(CORNER_ARM_START, FRAME_FAR_INSET)
        lineTo(FRAME_FAR_INSET, FRAME_FAR_INSET)
        lineTo(FRAME_FAR_INSET, CORNER_ARM_START)
        // 横扫线
        moveTo(SCAN_LINE_START, MIDDLE)
        lineTo(SCAN_LINE_END, MIDDLE)
    }.build()

private val ICON_SIZE = 24.dp
private const val VIEWPORT_SIZE = 24f
private const val STROKE_WIDTH = 2f
private const val FRAME_INSET = 3f
private const val FRAME_FAR_INSET = 21f
private const val CORNER_ARM_START = 16f
private const val CORNER_ARM_END = 8f
private const val MIDDLE = 12f
private const val SCAN_LINE_START = 6f
private const val SCAN_LINE_END = 18f
