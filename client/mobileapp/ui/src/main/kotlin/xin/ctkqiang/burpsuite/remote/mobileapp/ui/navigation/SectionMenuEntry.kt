package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.annotation.StringRes

/**
 * 分区索引屏里的一行。
 *
 * @property labelResource 这一行显示的名字。
 * @property route 点这一行落到的路由。
 */
data class SectionMenuEntry(
    @StringRes val labelResource: Int,
    val route: String,
)
