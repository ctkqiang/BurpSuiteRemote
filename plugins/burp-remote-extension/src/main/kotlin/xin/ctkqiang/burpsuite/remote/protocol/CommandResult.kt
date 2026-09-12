// 命令执行结局：成功、被拒绝、执行失败。

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 一次命令执行的结果。
 *
 * 用封闭类型而不是异常表达：被拒绝与执行失败都是预期内的领域结果，只有真正的缺陷才该抛异常（rules.md §7.3）。
 */
@Serializable
sealed interface CommandResult {
    /**
     * 命令确实执行完成。
     *
     * @property operationIdentifier 本次执行的操作标识，客户端据此与发出的命令对账。
     */
    @Serializable
    @SerialName("succeeded")
    data class Succeeded(val operationIdentifier: OperationIdentifier) : CommandResult

    /**
     * 命令被有意拒绝；重试不会改变结局。
     *
     * @property reason 拒绝原因。
     */
    @Serializable
    @SerialName("rejected")
    data class Rejected(val reason: RejectionReason) : CommandResult

    /**
     * 命令执行失败。
     *
     * @property error 失败原因，含错误码与是否可重试。
     */
    @Serializable
    @SerialName("failed")
    data class Failed(val error: RemoteError) : CommandResult
}
