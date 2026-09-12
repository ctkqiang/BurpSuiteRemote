package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

/** 截图的处理阶段；取值来自 plan §30，终态只有就绪与失败两种。 */
enum class ScreenshotProcessingState {
    /** 已导入原始图。 */
    Imported,

    /** 正在分析画面结构。 */
    Analyzing,

    /** 已识别出内容区域。 */
    Detected,

    /** 正在美化。 */
    Beautifying,

    /** 美化结果可用。 */
    Ready,

    /** 处理失败；原始图仍在。 */
    Failed,
}
