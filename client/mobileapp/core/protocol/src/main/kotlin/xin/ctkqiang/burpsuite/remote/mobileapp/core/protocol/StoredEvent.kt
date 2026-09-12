// 事件日志里落盘的一条事件。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EpochMillisecondsInstantSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EventIdentifierSerializer
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.EventTypeSerializer
import java.time.Instant

/**
 * 本地事件日志中的一条事件（plan §12）；日志只追加，所以这里没有可变字段。
 *
 * @property eventIdentifier 事件身份，日志内唯一。
 * @property sequenceNumber 权威侧分配的单调序号，日志内唯一。
 * @property aggregate 事件归属的聚合。
 * @property eventType 事件的线上类型串。
 * @property eventVersion 载荷结构的版本，用来在结构变化后仍能重放旧事件。
 * @property timestamp 事件发生时刻，毫秒整数。
 * @property payload 载荷原始字节；大报文按标识符另行取回，日志里只留最小事实。
 * @property metadata 事件附带的可扩展元数据。
 */
@Serializable
data class StoredEvent(
    @Serializable(with = EventIdentifierSerializer::class)
    val eventIdentifier: EventIdentifier,
    val sequenceNumber: Long,
    val aggregate: AggregateReference,
    @Serializable(with = EventTypeSerializer::class)
    val eventType: EventType,
    val eventVersion: Int,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    val timestamp: Instant,
    val payload: ByteArray,
    val metadata: EventMetadata,
)
