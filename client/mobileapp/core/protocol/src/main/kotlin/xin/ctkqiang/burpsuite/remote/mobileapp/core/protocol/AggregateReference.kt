// 聚合类型与聚合实例的配对。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.AggregateIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.AggregateIdentifierSerializer

/**
 * 事件归属的聚合：类型与实例缺一不可，只有一个都定位不到聚合。
 *
 * @property aggregateType 聚合类型。
 * @property aggregateIdentifier 聚合实例。
 */
@Serializable
data class AggregateReference(
    val aggregateType: AggregateType,
    @Serializable(with = AggregateIdentifierSerializer::class)
    val aggregateIdentifier: AggregateIdentifier,
)
