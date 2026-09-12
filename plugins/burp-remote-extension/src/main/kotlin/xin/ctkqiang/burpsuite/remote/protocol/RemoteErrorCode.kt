/**
 * Burp Remote —— 协议层 / 错误码
 *
 * 声明命令执行失败时的机器可读错误码。失败与拒绝不同：拒绝重试多少次都一样，失败则
 * 可能只是环境暂时不配合，因此错误码必须携带「是否值得重试」这一信息。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 命令执行失败的错误码。
 *
 * 这里刻意不携带任何自由文本的说明。插件运行在 Burp 进程内，能看到的上下文包含请求
 * 正文、Cookie 与 Authorization 头，一旦允许把这类内容拼进错误描述，它就必然会被写进
 * 日志、写进响应、写进客户端的崩溃报告，而这三条路径都不该承载用户的流量内容。
 *
 * 因此错误只由「码」与「是否可重试」构成，排查所需的具体上下文由本地日志承担，且日志
 * 默认不记录敏感字段。
 */
@Serializable
enum class RemoteErrorCode {
    /** 插件内部出现了未被预期的状态。这是缺陷，需要附上本地日志排查。 */
    @SerialName("internal_failure")
    InternalFailure,

    /** 与 Burp 运行时交互失败，例如扩展尚未完成加载或接口调用被拒绝。 */
    @SerialName("burp_runtime_failure")
    BurpRuntimeFailure,

    /** 报文无法被正确编解码，通常是两端协议版本不一致所致。 */
    @SerialName("serialization_failure")
    SerializationFailure,

    /** 操作在限定时间内没有完成。环境可能只是繁忙，重试是合理选择。 */
    @SerialName("timeout")
    Timeout,
}
