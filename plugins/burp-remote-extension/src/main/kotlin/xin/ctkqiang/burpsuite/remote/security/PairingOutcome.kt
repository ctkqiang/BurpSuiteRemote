// 配对结局：成立并分配身份，或被拒绝并给出机器码。

package xin.ctkqiang.burpsuite.remote.security

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason

/**
 * 配对尝试的结局。
 *
 * 只有成立与拒绝两种，中间态会让上层做出错误的乐观判断；抄错码、票据过期都属于正常操作。
 */
sealed interface PairingOutcome {
    /**
     * 配对成立，插件已为该设备分配身份。
     *
     * 这里只表示判定成立；`DevicePaired` 写进事件日志是命令层的职责，事件必须由确认成功的发布者产生。
     *
     * @property deviceIdentifier 分配到的设备身份。
     */
    data class Succeeded(val deviceIdentifier: DeviceIdentifier) : PairingOutcome

    /**
     * 配对被拒绝。
     *
     * 复用协议层的拒绝原因枚举，让配对失败与控制命令失败在客户端共用同一套机器码。
     *
     * @property reason 拒绝原因。
     */
    data class Rejected(val reason: RejectionReason) : PairingOutcome
}
