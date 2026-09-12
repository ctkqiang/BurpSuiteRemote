// 审计日志：只记身份、命令类别与结局码。

package xin.ctkqiang.burpsuite.remote.transport

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.OperationIdentifier

/**
 * 审计日志。
 *
 * 方法签名里根本没有请求体、响应体、Cookie、Authorization 头或令牌的位置，敏感内容因此无从流入日志（rules.md §12）；
 * 写入目标由构造器注入，生产环境接 Burp 输出面板，测试接一个收集器。
 */
class RemoteAuditLogger(private val auditSink: (String) -> Unit) {
    /**
     * 记录一次控制命令的结局。
     *
     * 只接受标识与结局码：设备身份与操作标识已经在界面上可见，不属于机密，而内容本身不在参数列表里。
     */
    fun recordCommandOutcome(
        deviceIdentifier: DeviceIdentifier?,
        commandType: String,
        operationIdentifier: OperationIdentifier?,
        outcomeCode: String,
    ) {
        val representedDevice = deviceIdentifier?.value ?: UNAUTHENTICATED_DEVICE_TEXT
        val representedOperation = operationIdentifier?.value ?: ABSENT_OPERATION_TEXT
        auditSink("审计：设备=$representedDevice 命令=$commandType 操作=$representedOperation 结果=$outcomeCode")
    }

    private companion object {
        private const val UNAUTHENTICATED_DEVICE_TEXT = "未认证"

        private const val ABSENT_OPERATION_TEXT = "缺失"
    }
}
