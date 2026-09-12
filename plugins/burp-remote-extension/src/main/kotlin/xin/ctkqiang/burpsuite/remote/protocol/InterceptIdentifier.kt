/**
 * Burp Remote —— 协议层 / 身份
 *
 * 声明「一个被拦截项」的身份类型。放行、丢弃、修改三类命令都以它为操作对象，
 * 一旦身份不唯一，「放行 A 却丢弃了 B」将成为可能。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识 Burp 运行时持有的一个被拦截项。
 *
 * 被拦截项的生命周期极短：它只存在于请求被挂起、等待人工决定的这段时间内。因此该
 * 标识符必须由插件在挂起发生的那一刻分配并立即广播，客户端随后引用它下发决策。
 *
 * 取值在同一个被拦截项存续期间保持不变，被放行或丢弃后即作废。对已作废的标识符
 * 再次下发决策必须被识别为幂等重试，而不是新的操作。
 *
 * 序列化时按底层文本透明写出。
 *
 * @property value 被拦截项的唯一文本标识符，协议示例约定使用 `intercept_` 前缀。
 */
@JvmInline
@Serializable
value class InterceptIdentifier(val value: String)
