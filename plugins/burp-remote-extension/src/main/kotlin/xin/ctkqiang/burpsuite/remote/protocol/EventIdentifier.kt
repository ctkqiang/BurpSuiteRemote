/**
 * Burp Remote —— 协议层 / 身份
 *
 * 声明事件日志中「事件」的身份类型。身份必须由类型系统承载，而不是用裸文本传递：
 * 一旦全部用 String，把事件标识符误传到设备标识符的位置不会被编译器发现，只会在
 * 运行期的去重与续传逻辑里静默出错。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识只追加事件日志中的一条事件。
 *
 * 该值是「事实」的稳定引用，并与序号一起构成客户端的去重键。网络只保证至少一次投递，
 * 同一事件完全可能被重复送达，客户端必须依赖事件标识符与序号判断它是否已经处理过，
 * 而不能依赖到达顺序或消息数量。
 *
 * 取值由插件在事实发生的那一刻分配，在整个事件日志生命周期内保持不变。客户端绝不
 * 自行编造该值——只有权威运行时才有资格宣告事实已经发生。
 *
 * 序列化时按底层文本透明写出，因此线上表现为 `"eventIdentifier": "event_01J"`，
 * 而不是嵌套对象。
 *
 * @property value 事件的唯一文本标识符，协议示例约定使用 `event_` 前缀。
 */
@JvmInline
@Serializable
value class EventIdentifier(val value: String)
