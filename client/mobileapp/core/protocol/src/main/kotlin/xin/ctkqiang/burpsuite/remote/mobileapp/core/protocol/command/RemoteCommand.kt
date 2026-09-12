// 客户端可以发出的命令。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier

/** 命令表达意图，不保证结果；每条命令都带执行身份，重试靠它幂等。 */
sealed interface RemoteCommand {
    /** 本次执行的身份；同一个值只允许产生一次副作用。 */
    val operationIdentifier: OperationIdentifier
}
