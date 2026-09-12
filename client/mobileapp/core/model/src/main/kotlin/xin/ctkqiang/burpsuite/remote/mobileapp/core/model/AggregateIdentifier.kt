// 聚合实例的身份类型。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.model

/**
 * 标识一个聚合实例；单独出现不完整，要和聚合类型成对才说得通。
 *
 * @property value 文本取值。
 */
@JvmInline
value class AggregateIdentifier(val value: String)
