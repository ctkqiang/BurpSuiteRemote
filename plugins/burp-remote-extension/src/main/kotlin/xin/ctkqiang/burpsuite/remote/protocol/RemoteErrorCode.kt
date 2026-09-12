// 命令执行失败时的机器可读错误码。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 命令执行失败的错误码。只带码和「能否重试」，不带自由文本，免得把流量内容带进日志。 */
@Serializable
enum class RemoteErrorCode {
    /** 内部故障。 */
    @SerialName("internal_failure")
    InternalFailure,

    /** Burp 运行时故障。 */
    @SerialName("burp_runtime_failure")
    BurpRuntimeFailure,

    /** 序列化故障。 */
    @SerialName("serialization_failure")
    SerializationFailure,

    /** 执行超时。 */
    @SerialName("timeout")
    Timeout,
}
