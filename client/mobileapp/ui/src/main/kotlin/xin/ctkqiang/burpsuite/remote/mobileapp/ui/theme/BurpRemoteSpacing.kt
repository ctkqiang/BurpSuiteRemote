package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.unit.dp

/** 间距栅格，全部是 4 的倍数；随手写 13.dp 这类值会让对齐在滚动时逐屏漂移。 */
object BurpRemoteSpacing {
    /** 4dp：紧邻元素之间。 */
    val ExtraSmall = 4.dp

    /** 8dp：同一组内的元素之间。 */
    val Small = 8.dp

    /** 12dp：组内行列之间。 */
    val Medium = 12.dp

    /** 16dp：卡片内边距、屏幕左右边距。 */
    val Large = 16.dp

    /** 24dp：分区之间。 */
    val ExtraLarge = 24.dp

    /** 32dp：页面顶部留白。 */
    val ExtraExtraLarge = 32.dp
}
