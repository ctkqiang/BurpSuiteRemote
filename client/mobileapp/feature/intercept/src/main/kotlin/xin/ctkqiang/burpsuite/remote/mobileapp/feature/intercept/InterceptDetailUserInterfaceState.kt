package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord

/**
 * 拦截项详情与编辑状态。
 *
 * 编辑框里的内容是本机草稿：客户端还没有拦截命令端口，因此 [canSendCommand] 此刻恒为 false，
 * 界面把原因写在按钮旁，而不是让用户以为改动已经生效（rules.md §5.1）。
 */
data class InterceptDetailUserInterfaceState(
    /** 这条拦截项；尚未读到或不存在时为空。 */
    val record: InterceptRecord? = null,
    /** 是否已经从仓库读到过一次结果。 */
    val hasLoaded: Boolean = false,
    /** 用户正在编辑的请求行。 */
    val requestLineInput: String = "",
    /** 用户正在编辑的请求头，一行一条。 */
    val requestHeadersInput: String = "",
    /** 放行、丢弃、提交修改这类控制命令此刻有没有可发的通路。 */
    val canSendCommand: Boolean = false,
)
