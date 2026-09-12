/**
 * Burp Remote —— 界面层 / 卡片
 *
 * 标签页中每一块内容的外壳。把「卡片长什么样」收成一个组件，而不是让每个区块各自拼一次
 * 边框、内边距与标题：三张卡片的几何必须完全一致，任何一处漏改都会让整页看起来像三个
 * 不同的人做的。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * 一张带标题的卡片：圆角表面，标题在上，正文在下。
 *
 * 本类不出现任何颜色：表面色与描边色由 [CardBorder] 在绘制时从组件自身的主题色推导，
 * 因此卡片在浅色与深色主题下自动呈现不同的表面，而不需要在这里分支。
 *
 * @param sectionHeadingText 卡片标题的文案，已按当前语言解析。
 * @param sectionContent 卡片正文。
 */
internal class SectionCard(
    sectionHeadingText: String,
    sectionContent: JComponent,
) : JPanel(BorderLayout()) {
    init {
        border = CardBorder()

        val cardBody = JPanel()
        cardBody.layout = BoxLayout(cardBody, BoxLayout.Y_AXIS)
        cardBody.add(buildSectionHeadingLabel(sectionHeadingText))
        cardBody.add(Box.createVerticalStrut(HEADING_SPACING))
        cardBody.add(sectionContent)

        // 正文钉在 NORTH 上：卡片高度取自内容的首选高度，而不是把内容拉伸到填满整张卡片。
        // 拉伸会让「已配对设备」这类只有一两行的卡片凭空多出大片空白。
        add(cardBody, BorderLayout.NORTH)
    }

    /**
     * 造出卡片标题控件。
     *
     * 标题只比正文大一档并加粗：它的作用是分组，不该抢走页面上真正的主角（配对码）的注意力。
     * 也不使用系统标题边框自带的小号字——那种字号不随界面字体缩放，用户调大字体之后标题会
     * 反而比正文还小，层级当场反转。
     *
     * @param sectionHeadingText 标题文案。
     */
    private fun buildSectionHeadingLabel(sectionHeadingText: String): JLabel {
        val headingLabel = JLabel(sectionHeadingText)
        val headingFont = headingLabel.font
        headingLabel.font = headingFont.deriveFont(Font.BOLD, headingFont.size2D + HEADING_FONT_SIZE_DELTA)
        headingLabel.alignmentX = Component.LEFT_ALIGNMENT
        return headingLabel
    }

    private companion object {
        /** 标题与正文之间的间距。 */
        private const val HEADING_SPACING = 6

        /** 标题字号相对正文字号的增量。 */
        private const val HEADING_FONT_SIZE_DELTA = 1f
    }
}
