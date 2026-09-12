// 操作日志的内存实现。

package xin.ctkqiang.burpsuite.remote.transport

import xin.ctkqiang.burpsuite.remote.protocol.CommandResult
import xin.ctkqiang.burpsuite.remote.protocol.OperationIdentifier
import java.util.concurrent.ConcurrentHashMap

/**
 * 操作日志的内存实现。
 *
 * 记录只增不改：一个操作标识的首个结果就是它永远的结果，这样重试才与首次执行等价。
 */
class InMemoryRemoteOperationLog : RemoteOperationLog {
    private val recordedResults = ConcurrentHashMap<OperationIdentifier, CommandResult>()

    override fun findRecordedResult(operationIdentifier: OperationIdentifier): CommandResult? =
        recordedResults[operationIdentifier]

    // computeIfAbsent 在同一个键上串行执行映射函数：并发重复到达时只有第一个会真正执行命令。
    override fun recordResultIfAbsent(
        operationIdentifier: OperationIdentifier,
        resultSupplier: () -> CommandResult,
    ): CommandResult = recordedResults.computeIfAbsent(operationIdentifier) { resultSupplier() }
}
