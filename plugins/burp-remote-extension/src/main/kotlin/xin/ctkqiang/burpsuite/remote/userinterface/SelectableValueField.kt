package xin.ctkqiang.burpsuite.remote.userinterface

import java.awt.Component
import java.awt.Insets
import javax.swing.JTextField

// 用 JTextField 而非 JLabel：文本字段能选中并按 Ctrl+C，机器值不该只能靠肉眼抄。
// 关掉不透明与边框，免得它长得像输入框，让人以为能打字。
internal class SelectableValueField(columns: Int = CONTENT_WIDTH_COLUMNS) : JTextField(columns) {
    init {
        isEditable = false
        isOpaque = false
        border = null
        margin = Insets(0, 0, 0, 0)
        // 显式左对齐：宽度不随容器伸展的控件默认会被居中，取值会比上方的说明文字向右偏出一段。
        alignmentX = Component.LEFT_ALIGNMENT
    }

    private companion object {
        // 列数 0 表示按内容自适应：配对码长度由安全层决定，写死列数会让它调整之后末尾被裁掉。
        private const val CONTENT_WIDTH_COLUMNS = 0
    }
}
