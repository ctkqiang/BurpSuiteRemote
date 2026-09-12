/**
 * Burp Remote —— 界面层 / 卡片边框
 *
 * 提供标签页中每一块内容的外框：圆角表面、细描边与统一内边距。
 *
 * 为什么自己画而不用带标题的边框：系统自带的标题边框是方角蚀刻线，嵌在深色主题里会出现
 * 一道浅色凹槽；而圆角表面既能把「哪几行属于同一件事」交给视觉传达，又能在浅色与深色主题
 * 下都保持同一副样子。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.border.AbstractBorder

/**
 * 圆角卡片边框。
 *
 * 颜色在绘制时才从组件自身的背景色与前景色推导，而不是在构造期读取或写死：
 *
 * - Burp 在扩展构造完成之后才把主题应用到组件上，构造期读到的取值可能尚未主题化；
 * - 写死颜色则必然在其中一种主题下失效——浅色主题需要「比背景稍深」的表面，
 *   深色主题需要「比背景稍浅」的表面，同一个灰值不可能同时满足两者。
 *
 * 因此本类型自身不持有任何颜色，只持有几何参数。它因此也不需要在主题切换时重建。
 *
 * @param padding 四周内边距。卡片内容与边框之间留出固定呼吸感，是这一层唯一的可调参数；
 *   把内边距收进边框而不是在每个卡片里各加一圈空边距，是为了让三张卡片的内边距永远一致。
 */
internal class CardBorder(
    private val padding: Int = CARD_PADDING,
) : AbstractBorder() {
    /**
     * 返回单参数形式的内边距。
     */
    override fun getBorderInsets(component: Component): Insets = Insets(padding, padding, padding, padding)

    /**
     * 返回双参数形式的内边距。
     *
     * 必须与单参数版本一并覆写：`AbstractBorder` 的默认实现只是把传入的 `Insets` 归零，
     * 并不会转调单参数版本。部分 Look and Feel 走的正是这个入口，只覆写一个会让卡片在
     * 某些主题下突然失去内边距，而那种缺失只在特定主题下出现，极难复现。
     */
    override fun getBorderInsets(
        component: Component,
        insets: Insets,
    ): Insets {
        insets.set(padding, padding, padding, padding)
        return insets
    }

    /**
     * 绘制卡片表面与描边。
     *
     * 边框的绘制发生在组件自身背景之后、子组件之前，因此这里填充的圆角表面天然位于内容
     * 之下：卡片既得到了圆角，内容也不必被裁剪。
     */
    override fun paintBorder(
        component: Component,
        graphics: Graphics,
        borderX: Int,
        borderY: Int,
        borderWidth: Int,
        borderHeight: Int,
    ) {
        // Swing 始终传入 Graphics2D；圆角与描边需要抗锯齿，否则边缘会出现阶梯状锯齿，
        // 而那恰恰会让「圆角」看起来像画歪了的方角。
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
            // 描边向内收一个像素：线宽为 1 时，落在整数坐标上的线会跨在边界两侧，
            // 一半被裁掉之后看起来比设定的更细、也更模糊。
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

    /**
     * 把前景色按给定比例混入背景色。
     *
     * 用比例混合而不是调整明度：`darker()` 与 `brighter()` 在深色主题下会把表面压成纯黑，
     * 在浅色主题下又可能亮到看不见边界；按比例掺入主题自己的前景色，则会随着主题变化
     * 自动得到「比背景略深（浅色主题）」或「比背景略浅（深色主题）」的一致效果。
     *
     * @param baseColour 作为底色的组件背景色。
     * @param accentColour 掺入的前景色。
     * @param accentWeight 掺入比例，取值在 0 与 1 之间。
     * @return 混合后的颜色。
     */
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
        /** 卡片四周的内边距。 */
        private const val CARD_PADDING = 16

        /** 圆角直径。取值克制——过度圆角会让一张信息卡片看起来像按钮。 */
        private const val CORNER_DIAMETER = 12

        /** 表面掺入前景色的比例。轻到只是「与页面底色略作区分」，而不会喧宾夺主。 */
        private const val SURFACE_ACCENT_WEIGHT = 0.05f

        /** 描边掺入前景色的比例。比表面重，让卡片边界在两种主题下都清晰可辨。 */
        private const val OUTLINE_ACCENT_WEIGHT = 0.18f
    }
}
