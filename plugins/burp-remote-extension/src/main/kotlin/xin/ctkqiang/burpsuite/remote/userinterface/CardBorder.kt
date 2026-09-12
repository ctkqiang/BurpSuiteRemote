package xin.ctkqiang.burpsuite.remote.userinterface

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.border.AbstractBorder

// 不填充、只描边。卡片内部全是普通 JPanel，它们会用页面底色盖住任何卡面填充，最后只在圆角
// 周围留下一圈色差，看着像个空心框；描边没有这个问题，也不会因为以后往卡片里加面板而复发。
internal class CardBorder(
    private val padding: Int = CARD_PADDING,
) : AbstractBorder() {
    override fun getBorderInsets(component: Component): Insets = Insets(padding, padding, padding, padding)

    // AbstractBorder 的默认实现只把传入的 Insets 归零，不会转调单参版本，因此必须一并覆写。
    override fun getBorderInsets(
        component: Component,
        insets: Insets,
    ): Insets {
        insets.set(padding, padding, padding, padding)
        return insets
    }

    override fun paintBorder(
        component: Component,
        graphics: Graphics,
        borderX: Int,
        borderY: Int,
        borderWidth: Int,
        borderHeight: Int,
    ) {
        // 圆角与描边需要抗锯齿，否则阶梯状锯齿会让圆角看起来像画歪的方角。
        val graphics2d = graphics.create() as Graphics2D

        try {
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            graphics2d.color = blend(component.background, component.foreground, OUTLINE_ACCENT_WEIGHT)
            // 描边收进一个像素：线宽 1 落在整数坐标上会跨在边界两侧，被裁掉一半后比设定更细、更模糊。
            graphics2d.drawRoundRect(
                borderX,
                borderY,
                borderWidth - 1,
                borderHeight - 1,
                CORNER_DIAMETER,
                CORNER_DIAMETER,
            )
        } finally {
            graphics2d.dispose()
        }
    }

    // 不用 darker()/brighter()：深色主题下会把颜色压成纯黑，浅色主题下又可能亮到看不见边界。
    private fun blend(
        baseColour: Color,
        accentColour: Color,
        accentWeight: Float,
    ): Color {
        val baseWeight = 1f - accentWeight
        return Color(
            (baseColour.red * baseWeight + accentColour.red * accentWeight).toInt(),
            (baseColour.green * baseWeight + accentColour.green * accentWeight).toInt(),
            (baseColour.blue * baseWeight + accentColour.blue * accentWeight).toInt(),
        )
    }

    private companion object {
        private const val CARD_PADDING = 16

        // 克制些：过度圆角会让信息卡片看起来像按钮。
        private const val CORNER_DIAMETER = 12

        // 描边取样比例：浅色主题下得到一条深一点的灰线，深色主题下得到浅一点的，两边都看得见边界。
        private const val OUTLINE_ACCENT_WEIGHT = 0.18f
    }
}
