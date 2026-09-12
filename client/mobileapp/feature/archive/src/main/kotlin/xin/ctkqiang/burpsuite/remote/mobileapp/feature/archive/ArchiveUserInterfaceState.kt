package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import java.time.Instant

/**
 * 归档屏状态（plan §22）。
 *
 * 「已保存」页签读的是 [HistoryRecord.archiveState] 为已归档的那部分投影；书签与截图两类投影
 * 客户端还没有端口，因此界面对它们只说明缺口，不显示占位条目（rules.md §5.1）。
 */
data class ArchiveUserInterfaceState(
    /** 当前页签。 */
    val selectedTab: ArchiveTab = ArchiveTab.SavedHistory,
    /** 已归档的历史记录，按事件序号升序。 */
    val savedRecords: List<HistoryRecord> = emptyList(),
    /** 相对时间的参照点；由 ViewModel 注入的时钟给出。 */
    val now: Instant = Instant.EPOCH,
    /** 是否已经从仓库读到过一次结果。 */
    val hasLoaded: Boolean = false,
    /** 本次刷新是否还在进行。 */
    val isRefreshing: Boolean = false,
    /** 读取失败的原因；成功时为 null。 */
    @StringRes val failureReasonResource: Int? = null,
)
