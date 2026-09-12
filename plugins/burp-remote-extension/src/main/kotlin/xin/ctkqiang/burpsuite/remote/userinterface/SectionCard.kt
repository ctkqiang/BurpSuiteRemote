package xin.ctkqiang.burpsuite.remote.userinterface

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

// 本类不出现任何颜色：表面与描边由 CardBorder 从组件主题色推导，浅色与深色主题自动区分。
internal class SectionCard(
    sectionHeadingText: String,
    sectionContent: JComponent,
) : JPanel(BorderLayout()) {
    init {
        // 不透明的话 JPanel 会先铺满一个方角背景，而圆角表面盖不住四个角，四角会露出方色块。
        isOpaque = false
        border = CardBorder()

        val cardBody = JPanel()
        cardBody.layout = BoxLayout(cardBody, BoxLayout.Y_AXIS)
        cardBody.add(buildSectionHeadingLabel(sectionHeadingText))
        cardBody.add(Box.createVerticalStrut(HEADING_SPACING))
        cardBody.add(sectionContent)

        // 钉在 NORTH：卡片高度取内容的首选高度，拉伸填满会让只有一两行的卡片凭空多出大片空白。
        add(cardBody, BorderLayout.NORTH)
    }

    // 标题只比正文大一档并加粗，不抢配对码的注意力；也不用系统标题边框的小号字，那种字号不随界面字体缩放。
    private fun buildSectionHeadingLabel(sectionHeadingText: String): JLabel {
        val headingLabel = JLabel(sectionHeadingText)
        val headingFont = headingLabel.font
        headingLabel.font = headingFont.deriveFont(Font.BOLD, headingFont.size2D + HEADING_FONT_SIZE_DELTA)
        headingLabel.alignmentX = Component.LEFT_ALIGNMENT
        return headingLabel
    }

    private companion object {
        private const val HEADING_SPACING = 6

        private const val HEADING_FONT_SIZE_DELTA = 1f
    }
}
