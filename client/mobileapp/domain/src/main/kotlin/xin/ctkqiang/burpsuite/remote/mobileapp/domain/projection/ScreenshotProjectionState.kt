package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.ScreenshotIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ScreenshotRecord

/**
 * 截图投影的状态。
 *
 * [appliedEventIdentifiers] 让折叠天然幂等；它只活在内存里，因此重建永远是从空状态重放整份日志。
 */
data class ScreenshotProjectionState(
    /** 已经折入过的事件身份。 */
    val appliedEventIdentifiers: Set<EventIdentifier> = emptySet(),
    /** 截图，按身份索引。 */
    val recordsByIdentifier: Map<ScreenshotIdentifier, ScreenshotRecord> = emptyMap(),
)
