package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 结构性尺寸与触控目标。
 *
 * 这些值不属于某一屏：顶栏、底栏、图标、触控区一旦各写各的，切页时尺寸就会一屏一个样。
 */
object BurpRemoteSizing {
    /** 48dp：可点元素的最小触控边长。 */
    val MinimumTouchTarget = 48.dp

    /** 24dp：默认图标边长，顶栏与底栏都用它。 */
    val Icon = 24.dp

    /** 20dp：行内图标，比默认档小一号。 */
    val InlineIcon = 20.dp

    /** 32dp：空态与错误态的大图标。 */
    val StateIcon = 32.dp

    /** 60dp：顶栏高度，恒定；跟着内容变会让正文在切页时上下跳。 */
    val TopBarHeight = 60.dp

    /** 60dp：底栏高度，恒定。 */
    val BottomBarHeight = 60.dp

    /** 1dp：顶栏下沿描边，恒定存在。 */
    val Divider = 1.dp

    /** 12dp：顶栏图标一侧的内边距。 */
    val TopBarIconPadding = 12.dp

    /** 20dp：顶栏标题一侧的内边距。 */
    val TopBarTitlePadding = 20.dp

    /** 20dp：底栏左右外边距，四项因此左右对称。 */
    val BottomBarHorizontalMargin = 20.dp

    /** 12dp：底栏与手势条之间的间隙；抬起来才不压在手势条上。 */
    val BottomBarGestureGap = 12.dp

    /** 6dp：选中胶囊相对底栏上下、左右的内缩。 */
    val BottomBarIndicatorInset = 6.dp
}
