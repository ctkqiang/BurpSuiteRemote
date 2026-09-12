// Burp 观察到一条历史记录这一事实。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.event

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EpochMillisecondsInstantSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EventIdentifierSerializer
import java.time.Instant

/**
 * Burp 观察到一条 HTTP 历史记录；报文本体按标识符另行取回，不走事件流。
 *
 * @property eventIdentifier 事件身份，与序号一起组成去重键。
 * @property sequenceNumber 权威侧分配的单调序号。
 * @property occurredAt 事件发生时刻，毫秒整数。
 */
@Serializable
data class HistoryItemObserved(
    @Serializable(with = EventIdentifierSerializer::class)
    override val eventIdentifier: EventIdentifier,
    override val sequenceNumber: Long,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    override val occurredAt: Instant,
) : HistoryEvent {
    /** 本事件的线上类型串。 */
    override val eventType: EventType
        get() = EventType.HISTORY_ITEM_OBSERVED
}
