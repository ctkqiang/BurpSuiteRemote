/**
 * Burp Remote —— 界面层 / 二维码
 *
 * 绘制二维码。二维码的对比度由标准决定，与 Burp 的主题无关，因此本文件是界面层里唯一
 * 写死颜色的地方；理由都写在写入颜色的那一行旁边。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import com.google.zxing.common.BitMatrix
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JComponent

/**
 * 把二维码模块矩阵绘制到界面上。
 *
 * 组件自行绘制，而不是把矩阵转成 `java.awt.image.BufferedImage` 再交给 `JLabel`：位图的
 * 像素在生成那一刻就固定了，而 Burp 允许用户放大界面字体，缩放位图会引入插值，模块边缘
 * 变成渐变灰边——那正是扫描失败最常见的原因。按当前尺寸重新计算整数倍缩放，则任何缩放
 * 下模块都是实心方块。
 *
 * @param qrCodeModules 二维码模块矩阵，深色模块为真。
 */
internal class PairingQrCodeView(private val qrCodeModules: BitMatrix) : JComponent() {
    init {
        // 不透明且背景固定为白色：二维码标准要求深色模块位于浅色底上，并且四周留有静默区。
        // 若让深色主题的背景透出来，扫描器连图案边界都无法确定。
        isOpaque = true
        background = Color.WHITE
        preferredSize = Dimension(PREFERRED_SIDE, PREFERRED_SIDE)
    }

    /**
     * 绘制二维码本体。
     *
     * @param graphics 由 Swing 提供的绘制上下文。绘制只使用整数坐标，因此不会产生半像素。
     */
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

    /**
     * 计算每个模块在当前尺寸下应占的像素边长。
     *
     * 取整数倍而不是按比例缩放，是为了让模块始终保持正方形；容器变小时至少保留一个像素，
     * 否则绘制退化为空白，而空白二维码看起来只是「还没生成」，会掩盖真正的布局问题。
     */
    private fun computeModuleSide(): Int {
        val totalModules = qrCodeModules.width + QUIET_ZONE_MODULES * QUIET_ZONE_SIDES_PER_AXIS
        return (minOf(width, height) / totalModules).coerceAtLeast(1)
    }

    private companion object {
        /**
         * 组件的首选边长。
         *
         * 320 而不是 220：二维码的内容是整张票据（约 190 字节），模块数因此比一条短链接多出
         * 近一倍，边长不足时每个模块只能分到 3 个像素，手机在正常距离下难以稳定对焦。
         * 放大之后模块边长翻倍，代价只是这块界面更占地方——而它本来就该是这一页的主角。
         */
        private const val PREFERRED_SIDE = 320

        /**
         * 静默区宽度，单位为模块。
         *
         * 4 是二维码标准规定的最小值：扫描器依靠这段纯色边缘定位图案边界，少一格就会在
         * 背景复杂的屏幕上频繁识别失败。
         */
        private const val QUIET_ZONE_MODULES = 4

        /** 静默区在每条轴上出现两次——图案的左右两侧各一次。 */
        private const val QUIET_ZONE_SIDES_PER_AXIS = 2
    }
}
