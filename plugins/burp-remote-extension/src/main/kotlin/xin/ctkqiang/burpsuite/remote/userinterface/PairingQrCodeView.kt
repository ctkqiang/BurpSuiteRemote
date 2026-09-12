package xin.ctkqiang.burpsuite.remote.userinterface

import com.google.zxing.common.BitMatrix
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JComponent

// 自行绘制而不转成 BufferedImage：位图像素在生成时已固定，用户放大界面后缩放会引入灰边。
internal class PairingQrCodeView(private val qrCodeModules: BitMatrix) : JComponent() {
    init {
        // 背景固定为白色：标准要求深色模块落在浅色底上，深色主题的背景透出来会让扫描器定不到边界。
        isOpaque = true
        background = Color.WHITE
        preferredSize = Dimension(PREFERRED_SIDE, PREFERRED_SIDE)
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)

        val moduleSide = computeModuleSide()
        val quietZoneSide = QUIET_ZONE_MODULES * moduleSide
        val renderedSide =
            qrCodeModules.width * moduleSide + quietZoneSide * QUIET_ZONE_SIDES_PER_AXIS
        val originX = (width - renderedSide) / 2 + quietZoneSide
        val originY = (height - renderedSide) / 2 + quietZoneSide

        graphics.color = Color.BLACK
        for (rowIndex in 0 until qrCodeModules.height) {
            for (columnIndex in 0 until qrCodeModules.width) {
                if (qrCodeModules.get(columnIndex, rowIndex)) {
                    val moduleLeft = originX + columnIndex * moduleSide
                    val moduleTop = originY + rowIndex * moduleSide
                    graphics.fillRect(moduleLeft, moduleTop, moduleSide, moduleSide)
                }
            }
        }
    }

    // 整数倍才能让模块保持正方形；容器变小时至少留 1 像素，画成空白会掩盖真正的布局问题。
    private fun computeModuleSide(): Int {
        val totalModules = qrCodeModules.width + QUIET_ZONE_MODULES * QUIET_ZONE_SIDES_PER_AXIS
        return (minOf(width, height) / totalModules).coerceAtLeast(1)
    }

    private companion object {
        // 320 而非 220：票据约 190 字节，模块数接近短链接两倍，边长不足时每模块只剩 3 像素，手机难对焦。
        private const val PREFERRED_SIDE = 320

        // 4 是标准规定的最小静默区：扫描器靠它定位图案边界，少一格就会在复杂背景上频繁识别失败。
        private const val QUIET_ZONE_MODULES = 4

        // 静默区在每条轴上出现两次，图案左右各一次。
        private const val QUIET_ZONE_SIDES_PER_AXIS = 2
    }
}
