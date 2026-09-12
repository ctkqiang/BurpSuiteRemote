/**
 * Burp Remote —— 安全层 / 配对结果
 *
 * 声明一次配对尝试的结局。配对失败是预期内的领域结果而非缺陷——有人抄错一位、二维码
 * 在屏幕上放久了过期，都属于正常操作，因此它必须是一个封闭的类型，而不是一句异常信息。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.security

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason

/**
 * 配对尝试的结局。
 *
 * 只有两种：要么成立并分配身份，要么被拒绝并给出机器码。刻意不提供「部分成功」或
 * 「稍后重试」之类的中间态——配对要么完成了，要么没有，中间态只会让上层产生错误的
 * 乐观判断。
 */
sealed interface PairingOutcome {
    /**
     * 配对成立，插件已为该设备分配身份。
     *
     * 注意本类型只表示「判定成立」。把 `DevicePaired` 事件写入事件日志是命令层的职责
     * （plan §14）：事件是事实，必须由确认成功的发布者产生，而不能由判定逻辑顺手伪造。
     *
     * @property deviceIdentifier 插件为新配对设备分配的身份。
     */
    data class Succeeded(val deviceIdentifier: DeviceIdentifier) : PairingOutcome

    /**
     * 配对被拒绝。
     *
     * 复用协议层的拒绝原因枚举，使「配对被拒绝」与「控制命令被拒绝」在客户端共用同一套
     * 机器码，避免同一类失败在两端各维护一份需要翻译的措辞。
     *
     * @property reason 拒绝的具体原因。
     */
    data class Rejected(val reason: RejectionReason) : PairingOutcome
}
