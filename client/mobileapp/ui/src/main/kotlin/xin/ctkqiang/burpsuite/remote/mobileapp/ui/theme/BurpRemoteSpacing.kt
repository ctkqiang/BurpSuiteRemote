package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.unit.dp

/** 间距栅格，只允许 4/8/12/16/20/24/32；随手写 13.dp 这类值会让对齐在滚动时逐屏漂移。 */
object BurpRemoteSpacing {
    /** 4dp：紧邻元素之间。 */
    val ExtraSmall = 4.dp

    /** 8dp：同一组内的元素之间。 */
    val Small = 8.dp

    /** 12dp：组内行列之间。 */
    val Medium = 12.dp

    /** 16dp：卡片内边距，也是并排控件之间更松的一档。 */
    val Large = 16.dp

    /** 20dp：全应用统一的屏幕左右内边距。 */
    val ScreenEdge = 20.dp

    /** 24dp：分区之间。 */
    val ExtraLarge = 24.dp

    /** 32dp：页面顶部留白。 */
    val ExtraExtraLarge = 32.dp

    /** 14dp：列表行的垂直内边距；行高由它定，不跟着内容多寡变。 */
    val ListRowVertical = 14.dp

    /** 10dp：列表项之间；比区块内间距小、比紧邻间距大，一列才读得出是一条条的。 */
    val ListItemGap = 10.dp
}
