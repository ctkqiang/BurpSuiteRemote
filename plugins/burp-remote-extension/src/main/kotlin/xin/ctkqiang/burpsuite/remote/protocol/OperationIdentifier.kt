/**
 * Burp Remote —— 协议层 / 身份
 *
 * 声明「一次命令执行」的身份类型。命令可以重试，但重试绝不能产生第二次副作用：
 * 该身份是插件判定「这条命令我是否已经执行过」的唯一依据。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable

/**
 * 标识一次命令执行，使重试具备幂等性。
 *
 * 每条命令都必须携带该值。插件在命令执行结果写入操作日志后，对携带同一标识符的
 * 后续请求直接返回已记录的结果，而不是重新执行破坏性动作。若缺少它，一次网络抖动
 * 后的自动重试就可能把同一个请求重复放行到目标服务器。
 *
 * 取值由发起命令的一方生成，必须在整次重试周期内保持不变；每次新意图都必须使用
 * 新值，复用旧值会让新命令被误判为已执行。
 *
 * 序列化时按底层文本透明写出。
 *
 * @property value 命令执行的唯一文本标识符，协议示例约定使用 `operation_` 前缀。
 */
@JvmInline
@Serializable
value class OperationIdentifier(val value: String)
