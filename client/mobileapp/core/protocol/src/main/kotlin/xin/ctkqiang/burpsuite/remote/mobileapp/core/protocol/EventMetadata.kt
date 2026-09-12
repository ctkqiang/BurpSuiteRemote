// 事件附带的可扩展元数据。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.Serializable

/**
 * 事件携带的元数据；plan §11 只要求有这一列、没有定义字段，因此先用键值对承载。
 *
 * @property attributes 键值对；没有元数据时是空表。
 */
@Serializable
data class EventMetadata(val attributes: Map<String, String> = emptyMap())
