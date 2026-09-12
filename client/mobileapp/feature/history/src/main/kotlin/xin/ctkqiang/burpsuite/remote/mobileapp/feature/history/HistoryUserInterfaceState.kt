package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord

/**
 * 实时历史状态（plan §20）。
 *
 * 只带元数据：报文本体按标识另行取回，列表因此不必为每一行加载大报文。
 */
data class HistoryUserInterfaceState(val records: List<HistoryRecord> = emptyList())
