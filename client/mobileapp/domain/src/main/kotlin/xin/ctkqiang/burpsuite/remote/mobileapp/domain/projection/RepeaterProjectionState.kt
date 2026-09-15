// Repeater 投影的状态。

package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.RepeaterRecord

/**
 * Repeater 投影的状态。
 *
 * [appliedEventIdentifiers] 让折叠天然幂等；它只活在内存里，重建时永远从空状态重放整份日志。
 */
data class RepeaterProjectionState(
    /** 已经折入过的事件身份。 */
    val appliedEventIdentifiers: Set<EventIdentifier> = emptySet(),
    /** Repeater 请求，按身份索引。 */
    val recordsByIdentifier: Map<RepeaterRequestIdentifier, RepeaterRecord> = emptyMap(),
)
