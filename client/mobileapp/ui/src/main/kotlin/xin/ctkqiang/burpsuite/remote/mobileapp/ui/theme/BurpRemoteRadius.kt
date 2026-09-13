package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.unit.dp

/** 圆角档位。胶囊用一个大到不可能被分辨出直边的值，别拿具体宽高去凑。 */
object BurpRemoteRadius {
    /** 14dp：按钮、输入框等可交互控件。 */
    val Control = 14.dp

    /** 16dp：卡片。 */
    val Card = 16.dp

    /**
     * 30dp：浮动底栏。
     *
     * 底栏高 60dp，因此这一档正好是自身高度的一半——两端落在半径上限上，成为两个完整的半圆。
     * 超过这个值再多画也只是被裁掉，所以它是「圆到不能再圆」的那一档，而不是随便挑的大圆角。
     */
    val FloatingNavigation = 30.dp

    /** 24dp：弹窗与抽屉这类整块浮起的容器。 */
    val Dialog = 24.dp

    /** 20dp：扫码取景方框；取景区是相机画面里的一块，因而不跟弹窗同档。 */
    val Viewfinder = 20.dp

    /** 999dp：胶囊。 */
    val Capsule = 999.dp
}
