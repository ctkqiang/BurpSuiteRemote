package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.ScreenshotIdentifier
import java.time.Instant

/**
 * 界面与领域读的一条截图。
 *
 * 原始图与美化图共用同一个截图身份；处理失败时原始图仍在，因此两条 URI 是分开的列（plan §29）。
 */
data class ScreenshotRecord(
    /** 截图身份。 */
    val screenshotIdentifier: ScreenshotIdentifier,
    /** 截图导入的时刻。 */
    val createdAt: Instant,
    /** 截图的来源历史记录；导入型截图为 null。 */
    val historyIdentifier: HistoryIdentifier?,
    /** 原始图位置。 */
    val originalUri: String?,
    /** 美化图位置；未完成美化时为 null。 */
    val processedUri: String?,
    /** 原始图宽度。 */
    val width: Int?,
    /** 原始图高度。 */
    val height: Int?,
    /** 图像格式。 */
    val format: String?,
    /** 美化完成的时刻。 */
    val processedAt: Instant?,
    /** 处理程序版本，用来判断旧结果要不要重算。 */
    val processingVersion: Int?,
    /** 当前处理阶段。 */
    val processingState: ScreenshotProcessingState,
)
