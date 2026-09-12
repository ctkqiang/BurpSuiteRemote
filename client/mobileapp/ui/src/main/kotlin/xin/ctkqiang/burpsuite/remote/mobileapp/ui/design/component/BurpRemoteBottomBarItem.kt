package xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 浮动底栏的一项。
 *
 * @property route 点这一项要落到的路由；选中判定也用它，与 NavHost 里的路由是同一个字符串。
 * @property labelResource 这一项显示的名字。
 * @property icon 这一项的图标。
 */
data class BurpRemoteBottomBarItem(
    val route: String,
    @StringRes val labelResource: Int,
    val icon: ImageVector,
)
