package xin.ctkqiang.burpsuite.remote.userinterface

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.border.AbstractBorder

// 只持有几何参数、不持有颜色：主题在组件构造之后才应用，颜色只能等绘制时从组件自身推导。
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

            val surfaceColour = blend(component.background, component.foreground, SURFACE_ACCENT_WEIGHT)
            graphics2d.color = surfaceColour
            graphics2d.fillRoundRect(
                borderX,
                borderY,
                borderWidth,
                borderHeight,
                CORNER_DIAMETER,
                CORNER_DIAMETER,
            )

            val outlineColour = blend(component.background, component.foreground, OUTLINE_ACCENT_WEIGHT)
            graphics2d.color = outlineColour
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

    // 不用 darker()/brighter()：那在深色主题下会把表面压成纯黑，浅色主题下又可能亮到看不见边界。
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

        // 轻到只与页面底色略作区分，不喧宾夺主。
        private const val SURFACE_ACCENT_WEIGHT = 0.05f

        // 比表面重，让卡片边界在两种主题下都清晰可辨。
        private const val OUTLINE_ACCENT_WEIGHT = 0.18f
    }
}
