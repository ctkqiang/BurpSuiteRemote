/**
 * Burp Remote —— 界面层 / 可选中取值
 *
 * 展示机器值（配对码、设备身份、配对地址）的取值控件。这类值的共同点是「必须能被原样抄到
 * 另一台设备上」，因此它们的呈现方式必须统一，不能由各处自行决定。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import java.awt.Component
import java.awt.Insets
import javax.swing.JTextField

/**
 * 一个只读但可选中的取值控件：无边框、不透明，外观静止。
 *
 * 用它代替 `JLabel` 的理由很具体：文本字段可以用鼠标选中并按 Ctrl+C，而标签不行。机器值
 * 若只让操作者依赖「复制」按钮或肉眼抄写，等于把他的出错概率当成可接受成本。
 *
 * 关掉不透明与边框，是为了让它在视觉上仍然是一个静止的取值，而不是一个「等着你输入」的
 * 输入框——它一旦长得像输入框，操作者就会去点它，然后奇怪为什么打不了字。
 *
 * @param columns 首选宽度，以字符列数计；0 表示按当前内容自适应。
 */
internal class SelectableValueField(columns: Int = CONTENT_WIDTH_COLUMNS) : JTextField(columns) {
    init {
        isEditable = false
        isOpaque = false
        border = null
        margin = Insets(0, 0, 0, 0)
        // 显式左对齐。Swing 组件的默认横向对齐是「居中」，而一个宽度不能随容器伸展的控件在
        // 纵向布局里就会被居中摆放——取值会比它上方的说明文字向右偏出一段，看上去像排版错误。
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private companion object {
        /**
         * 首选宽度「按内容自适应」。
         *
         * 列数为 0 时宽度由当前内容算得。配对码的长度由安全层决定，在这里写死一个列数等于把
         * 那个长度复制了一遍——安全层一旦调整长度，界面就会把末尾几位裁掉，而裁掉的部分在
         * 屏幕上看起来「没问题」。
         */
        private const val CONTENT_WIDTH_COLUMNS = 0
    }
}
