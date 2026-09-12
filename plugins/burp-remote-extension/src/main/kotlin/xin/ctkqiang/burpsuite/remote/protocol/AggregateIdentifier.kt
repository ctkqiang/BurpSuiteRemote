/**
 * Burp Remote —— 协议层 / 身份
 *
 * 声明「一个聚合实例」的身份类型。事件信封中的 aggregateIdentifier 字段正是该类型的
 * 线上形态，它只有与 aggregateType 成对出现时才具备完整含义。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识一个聚合实例。
 *
 * 聚合标识符单独出现时是不完整的：`intercept_123` 这样的文本本身并不携带「它属于拦截项
 * 还是历史记录」的信息。若允许它脱离聚合类型四处流动，迟早会出现两种聚合类型的同名标识符
 * 被混为一谈的情况，而这种混淆在事件日志里表现为两条毫不相干的事件被串成一条状态迁移链，
 * 排查成本极高。
 *
 * 把身份收进值类型而非传递裸字符串，是为了让编译器参与拦截：接收方声明自己需要的是
 * 历史记录标识符时，调用方不可能误传聚合标识符，即便两者底层都是文本。这类错误在运行期
 * 几乎不可观测，只能靠类型系统在编译期挡住。
 *
 * @property value 聚合实例的唯一文本标识符，按协议示例由聚合类型前缀与随机后缀组成。
 */
@JvmInline
@Serializable
value class AggregateIdentifier(val value: String)
