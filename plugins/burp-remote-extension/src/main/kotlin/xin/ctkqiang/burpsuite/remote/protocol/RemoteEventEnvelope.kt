// 事件日志中一条事件的线上信封。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.time.Instant

/**
 * 一条事件的线上信封，字段名即协议契约（rules.md §5.3）。
 *
 * 只承载元数据：请求体与响应体按标识另行取，事件流才不会被大报文拖慢（plan §82）。
 *
 * @property protocolVersion 插件使用的协议版本。
 * @property eventIdentifier 事件身份，与序号一起构成客户端去重键。
 * @property sequenceNumber 单调递增序号，重连与续传都靠它对齐。
 * @property occurredAt 事件发生时刻，毫秒整数。
 * @property eventType 实现无关的事件类型，例如 history.item.observed。
 * @property aggregateType 事件所属的聚合类型。
 * @property aggregateIdentifier 事件所属的聚合实例。
 * @property payload 事件载荷；默认空对象，避免 null 在客户端多出一条分支。
 */
@Serializable
data class RemoteEventEnvelope(
    val protocolVersion: Int,
    val eventIdentifier: EventIdentifier,
    val sequenceNumber: Long,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    val occurredAt: Instant,
    val eventType: String,
    val aggregateType: AggregateType,
    val aggregateIdentifier: AggregateIdentifier,
    val payload: JsonElement = JsonObject(emptyMap()),
)
