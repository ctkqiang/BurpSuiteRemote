package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import java.time.Instant

/**
 * 实时历史状态（plan §20）。
 *
 * 只带元数据：报文本体按标识另行取回，列表因此不必为每一行加载大报文。
 * 「还没读出来」「读失败」「读出来是空的」分开表达，界面才不会拿空列表冒充「没有请求」。
 */
data class HistoryUserInterfaceState(
    /** 仍在实时投影里的记录，按事件序号升序。 */
    val records: List<HistoryRecord> = emptyList(),
    /** 相对时间的参照点；由 ViewModel 注入的时钟给出。 */
    val now: Instant = Instant.EPOCH,
    /** 是否已经从仓库读到过一次结果。 */
    val hasLoaded: Boolean = false,
    /** 本次刷新是否还在进行。 */
    val isRefreshing: Boolean = false,
    /** 读取失败的原因；成功时为 null。 */
    @StringRes val failureReasonResource: Int? = null,
)
