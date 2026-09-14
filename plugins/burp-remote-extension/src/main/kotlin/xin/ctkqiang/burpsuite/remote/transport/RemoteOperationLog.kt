// 操作日志端口：同一个操作标识只允许产生一次副作用。

package xin.ctkqiang.burpsuite.remote.transport

import xin.ctkqiang.burpsuite.remote.protocol.CommandResult
import xin.ctkqiang.burpsuite.remote.protocol.OperationIdentifier

/**
 * 操作日志端口（plan §16）。
 *
 * 抽成接口是为了给持久化留位置：换成磁盘实现时，命令网关与传输层都不用改。
 * 当前实现只在内存里，随扩展卸载消失——与控制命令的作用范围一致。
 */
interface RemoteOperationLog {
    /** 取该操作已记录的结果；从未执行过时返回 null。 */
    fun findRecordedResult(operationIdentifier: OperationIdentifier): CommandResult?

    /**
     * 首次到达时执行并记录结果；重复到达时直接返回首次记录的结果。
     *
     * 「查出不存在」与「执行并写入」必须是一个原子动作，否则同一操作标识并发到达时会被执行两次。
     * 执行动作必须拿到操作标识：成功结果里要回填它，客户端才能与自己的请求对上账。
     */
    fun recordResultIfAbsent(
        operationIdentifier: OperationIdentifier,
        resultSupplier: (OperationIdentifier) -> CommandResult,
    ): CommandResult
}
