package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

/**
 * 投影的名字。
 *
 * 校验点按它分开存放，所以每个投影必须有自己的名字，且名字一旦发布就不能再改（rules.md §5.6）。
 *
 * @property value 文本取值。
 */
@JvmInline
value class ProjectionName(val value: String) {
    companion object {
        /** HTTP 历史记录投影。 */
        val HISTORY: ProjectionName = ProjectionName("history")

        /** 拦截队列投影。 */
        val INTERCEPT: ProjectionName = ProjectionName("intercept")

        /** 截图投影。 */
        val SCREENSHOT: ProjectionName = ProjectionName("screenshot")
    }
}
