package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import androidx.annotation.StringRes
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import java.time.Instant

/**
 * 拦截队列状态（plan §43 的 Live/Intercept）。
 *
 * 列表只带元数据：报文本体按标识另取，队列长起来时列表不该为每一行加载大报文（plan §20）。
 * 三态分开表达，界面才不用空列表冒充「队列是空的」。
 */
data class InterceptUserInterfaceState(
    /** 队列里的拦截项，按事件序号升序。 */
    val records: List<InterceptRecord> = emptyList(),
    /** 相对时间的参照点；由 ViewModel 注入的时钟给出。 */
    val now: Instant = Instant.EPOCH,
    /** 是否已经从仓库读到过一次结果。 */
    val hasLoaded: Boolean = false,
    /** 本次刷新是否还在进行。 */
    val isRefreshing: Boolean = false,
    /** 读取失败的原因；成功时为 null。 */
    @StringRes val failureReasonResource: Int? = null,
)
