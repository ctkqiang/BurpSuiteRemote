// 控制命令的安全闸门：认证、操作标识、幂等、限流、审计。

package xin.ctkqiang.burpsuite.remote.transport

import xin.ctkqiang.burpsuite.remote.protocol.CommandResult
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.security.PairedDeviceRegistry

/**
 * 控制命令的安全闸门（rules.md §4.2、§12）。
 *
 * 判定顺序固定为「认证 → 操作标识存在 → 幂等查表 → 限流 → 执行并记录」。
 * 幂等查表排在限流之前，是刻意的：重试必须拿回首次结果，若被限流挡住，客户端将永远对不上账。
 */
class RemoteControlGate(
    private val pairedDeviceRegistry: PairedDeviceRegistry,
    private val operationLog: RemoteOperationLog,
    private val rateLimiter: RemoteDeviceRateLimiter,
    private val auditLogger: RemoteAuditLogger,
) {
    /**
     * 把请求里声明的设备身份解析成已认证身份。
     *
     * 局域网不等于可信网络，因此身份必须能在已配对登记处里找到；找不到一律按未配对处理。
     */
    fun resolveAuthenticatedDevice(submittedDeviceIdentifier: DeviceIdentifier?): DeviceIdentifier? =
        submittedDeviceIdentifier?.takeIf { candidateDeviceIdentifier ->
            pairedDeviceRegistry.snapshot().any { pairedDevice ->
                pairedDevice.deviceIdentifier == candidateDeviceIdentifier
            }
        }

    /**
     * 执行一条控制命令；重复到达时返回首次记录的结果而不重复执行。
     *
     * 未配对、缺操作标识、超出限流都会以拒绝收场，并写明原因码进审计日志。
     */
    fun handleControlCommand(
        submittedDeviceIdentifier: DeviceIdentifier?,
        submittedOperationIdentifier: OperationIdentifier?,
        commandType: String,
        executeCommand: () -> CommandResult,
    ): CommandResult {
        val authenticatedDevice = resolveAuthenticatedDevice(submittedDeviceIdentifier)
        if (authenticatedDevice == null) {
            auditUnacceptedCommand(submittedDeviceIdentifier, commandType, RejectionReason.DeviceNotPaired)
            return CommandResult.Rejected(RejectionReason.DeviceNotPaired)
        }
        if (submittedOperationIdentifier == null) {
            auditUnacceptedCommand(authenticatedDevice, commandType, RejectionReason.MissingOperationIdentifier)
            return CommandResult.Rejected(RejectionReason.MissingOperationIdentifier)
        }

        val commandResult = executeOrReplay(authenticatedDevice, submittedOperationIdentifier, executeCommand)
        auditLogger.recordCommandOutcome(
            authenticatedDevice,
            commandType,
            submittedOperationIdentifier,
            describeOutcomeCode(commandResult),
        )
        return commandResult
    }

    // 幂等在前、限流在后：重试要能拿回首次结果，不能被限流挡住。
    private fun executeOrReplay(
        authenticatedDevice: DeviceIdentifier,
        operationIdentifier: OperationIdentifier,
        executeCommand: () -> CommandResult,
    ): CommandResult {
        val recordedResult = operationLog.findRecordedResult(operationIdentifier)
        if (recordedResult != null) {
            return recordedResult
        }
        if (!rateLimiter.tryAcquireToken(authenticatedDevice)) {
            return CommandResult.Rejected(RejectionReason.RateLimitExceeded)
        }
        return operationLog.recordResultIfAbsent(operationIdentifier, executeCommand)
    }

    private fun auditUnacceptedCommand(
        submittedDeviceIdentifier: DeviceIdentifier?,
        commandType: String,
        rejectionReason: RejectionReason,
    ) {
        auditLogger.recordCommandOutcome(
            submittedDeviceIdentifier,
            commandType,
            null,
            rejectionReason.name,
        )
    }

    private fun describeOutcomeCode(commandResult: CommandResult): String =
        when (commandResult) {
            is CommandResult.Succeeded -> SUCCEEDED_OUTCOME_CODE
            is CommandResult.Rejected -> commandResult.reason.name
            is CommandResult.Failed -> commandResult.error.code.name
        }

    private companion object {
        private const val SUCCEEDED_OUTCOME_CODE = "succeeded"
    }
}
