// 命令执行的结果。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.serialization.OperationIdentifierSerializer

/**
 * 命令的执行结果；被拒绝是预期内的领域结果、失败是缺陷或环境问题，因此分开表达（rules.md §7.3）。
 */
@Serializable
sealed interface CommandResult {
    /** 命令确实执行完成；客户端据此与发出的命令对账。 */
    @Serializable
    @SerialName("succeeded")
    data class Succeeded(
        @Serializable(with = OperationIdentifierSerializer::class)
        val operationIdentifier: OperationIdentifier,
    ) : CommandResult

    /** 命令被拒绝；拒绝是确定性的，重试同样会被拒。 */
    @Serializable
    @SerialName("rejected")
    data class Rejected(val reason: RejectionReason) : CommandResult

    /** 命令执行失败；失败可能只是时机问题，重试是否有意义由 isRetryable 回答。 */
    @Serializable
    @SerialName("failed")
    data class Failed(val error: RemoteError) : CommandResult
}
