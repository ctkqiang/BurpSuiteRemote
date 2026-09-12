// 拦截项已被放行这一事实。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.event

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EpochMillisecondsInstantSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EventIdentifierSerializer
import java.time.Instant

/**
 * 拦截项已被放行；这是 Burp 真的把它送出去之后才产生的事实。
 *
 * @property eventIdentifier 事件身份，与序号一起组成去重键。
 * @property sequenceNumber 权威侧分配的单调序号。
 * @property occurredAt 事件发生时刻，毫秒整数。
 */
@Serializable
data class InterceptForwarded(
    @Serializable(with = EventIdentifierSerializer::class)
    override val eventIdentifier: EventIdentifier,
    override val sequenceNumber: Long,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    override val occurredAt: Instant,
) : InterceptEvent {
    /** 本事件的线上类型串。 */
    override val eventType: EventType
        get() = EventType.INTERCEPT_FORWARDED
}
