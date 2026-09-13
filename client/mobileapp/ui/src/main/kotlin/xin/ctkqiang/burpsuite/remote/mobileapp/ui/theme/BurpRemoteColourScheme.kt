package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 一套配色的全部槽位。
 *
 * 用固定的具名槽位而不是一个 Map：漏配一个槽位时编译不过，比运行时取到透明色再回头查图要好。
 *
 * @property background 页面底色；浅色下是纯白。
 * @property surface 卡片与分区底色。
 * @property surfaceElevated 浮起元素（浮底栏、弹层）的底色。
 * @property outline 描边与分隔线。
 * @property contentPrimary 正文与标题字色。
 * @property contentSecondary 次要说明字色。
 * @property accent 唯一强调色，用于可交互、选中与品牌标识。
 * @property onAccent 压在强调色上的字色。
 * @property success 成功语义色，赏金猎人快速判读用。
 * @property warning 警告语义色。
 * @property danger 危险语义色。
 * @property information 信息语义色。
 * @property scrim 遮罩色。
 * @property onScrim 压在遮罩上的字色；扫码取景是「黑遮罩 + 相机」，只能用浅色，不能跟着主题翻。
 * @property codeKey 代码高亮：键名。
 * @property codeString 代码高亮：字符串字面量。
 * @property codeNumber 代码高亮：数值字面量。
 * @property codeLiteral 代码高亮：`true` / `false` / `null` 这类关键字字面量。
 * @property codeComment 代码高亮：注释，也是整块代码里最该退到后面的那一层。
 */
data class BurpRemoteColourScheme(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val outline: Color,
    val contentPrimary: Color,
    val contentSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val information: Color,
    val scrim: Color,
    val onScrim: Color,
    val codeKey: Color,
    val codeString: Color,
    val codeNumber: Color,
    val codeLiteral: Color,
    val codeComment: Color,
)
