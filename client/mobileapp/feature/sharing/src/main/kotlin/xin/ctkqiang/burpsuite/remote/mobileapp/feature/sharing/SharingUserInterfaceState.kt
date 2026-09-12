package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord

/**
 * 分享与导出状态（plan §75）。
 *
 * 扫描与脱敏在记录到达时就算好：它们只是本地纯计算，没有网络也没有磁盘，不需要用户等。
 * 真正要用户点头的是最后一步——把导出副本写出去。
 */
data class SharingUserInterfaceState(
    /** 要导出的那条记录；尚未读到或不存在时为空。 */
    val record: HistoryRecord? = null,
    /** 是否已经从仓库读到过一次结果。 */
    val hasLoaded: Boolean = false,
    /** 扫描与脱敏结论；记录还没到时为空。 */
    val redaction: RedactionSummary? = null,
    /** 导出副本是否已经写好。 */
    val hasExported: Boolean = false,
    /** 上一次导出是否失败。 */
    val hasExportFailed: Boolean = false,
)
