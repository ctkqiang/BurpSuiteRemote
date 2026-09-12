package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord

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
)
