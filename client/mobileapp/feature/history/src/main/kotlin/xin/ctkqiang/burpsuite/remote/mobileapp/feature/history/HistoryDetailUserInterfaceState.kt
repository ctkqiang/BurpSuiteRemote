package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord

/**
 * 历史详情状态（plan §20、plan §22）。
 *
 * [hasLoaded] 把「还没读出来」和「读出来是空」分开：仓储在记录不存在时发 null，
 * 不区分这两件事的话界面会把「不存在」画成一直转圈。
 */
data class HistoryDetailUserInterfaceState(
    /** 这条记录；尚未读到或不存在时为空。 */
    val record: HistoryRecord? = null,
    /** 是否已经从仓库读到过一次结果。 */
    val hasLoaded: Boolean = false,
)
