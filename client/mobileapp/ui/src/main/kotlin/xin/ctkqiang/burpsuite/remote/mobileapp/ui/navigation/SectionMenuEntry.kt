package xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation

import androidx.annotation.StringRes

/**
 * 分区索引屏里的一行。
 *
 * [descriptionResource] 不是装饰：索引页的价值就在于点之前知道会去哪，一句话比一个名词更省一次往返。
 *
 * @property labelResource 这一行显示的名字。
 * @property descriptionResource 补一句它通向什么。
 * @property route 点这一行落到的路由。
 * @property groupResource 这一行所属分组的标题；为空时它自成一档且不画标题。
 */
data class SectionMenuEntry(
    @StringRes val labelResource: Int,
    @StringRes val descriptionResource: Int,
    val route: String,
    @StringRes val groupResource: Int? = null,
)
