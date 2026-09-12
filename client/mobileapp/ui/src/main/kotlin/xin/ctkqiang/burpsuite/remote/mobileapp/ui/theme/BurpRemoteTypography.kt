package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.text.TextStyle

/**
 * 字号只有五档，行高跟在每一档后面显式写死。
 *
 * 不引整套 Material 字阶：五档之外的字号一旦出现，层级就开始各写各的，界面很快就不再是一套版式。
 * [technical] 是等宽档——方法、主机、路径、状态码、标识符、序号、时间都必须能被逐字符比对。
 *
 * @property display 页面级标题。
 * @property title 分区标题。
 * @property body 正文。
 * @property label 标签与按钮文字。
 * @property technical 技术值，等宽。
 */
data class BurpRemoteTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val technical: TextStyle,
)
