// Repeater 执行已完成这一事实。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.event

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EpochMillisecondsInstantSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EventIdentifierSerializer
import java.time.Instant

/**
 * Repeater 执行已完成；响应本体按标识符另行取回，不走事件流（rules.md §11）。
 *
 * @property eventIdentifier 事件身份，与序号一起组成去重键。
 * @property sequenceNumber 权威侧分配的单调序号。
 * @property occurredAt 事件发生时刻，毫秒整数。
 */
@Serializable
data class RepeaterExecutionCompleted(
    @Serializable(with = EventIdentifierSerializer::class)
    override val eventIdentifier: EventIdentifier,
    override val sequenceNumber: Long,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    override val occurredAt: Instant,
) : RepeaterEvent {
    /** 本事件的线上类型串。 */
    override val eventType: EventType
        get() = EventType.REPEATER_EXECUTION_COMPLETED
}
