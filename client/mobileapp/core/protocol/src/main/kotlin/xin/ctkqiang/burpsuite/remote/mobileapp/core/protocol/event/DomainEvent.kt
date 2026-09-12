// 客户端观察到的一条事实。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.event

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import java.time.Instant

/** 客户端观察到的领域事件：只记录已经发生的事实，不表达意图；实现必须是不带可变状态的 data class。 */
sealed interface DomainEvent {
    /** 事件身份；与序号一起组成去重键。 */
    val eventIdentifier: EventIdentifier

    /** 权威侧分配的单调序号；发现断洞靠它。 */
    val sequenceNumber: Long

    /** 事件发生时刻，毫秒整数。 */
    val occurredAt: Instant

    /** 本事件的线上类型串。 */
    val eventType: EventType
}
