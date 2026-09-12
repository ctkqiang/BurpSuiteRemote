/**
 * Burp Remote —— 协议层 / 已配对设备
 *
 * 声明「一台已完成配对的设备」这一状态快照。插件要靠它回答一个安全问题——此刻究竟有哪些
 * 身份被允许执行控制命令——因此这份数据必须是可以拿出来审计的，而不是只存在于内存里的一串
 * 随机数（plan §2.2 把「已配对设备」列为插件持有的远程控制状态）。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.protocol

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * 一台已与插件完成配对的设备。
 *
 * 这是状态而不是事件：本类型描述「现在是什么样」，而「配对成立」「设备被移除」这两个事实
 * 由事件日志记录（plan §14）。两者不能互相替代——只有事件能回答「这台设备是从哪一刻起被
 * 允许的」这类审计问题，而只有状态能回答「此刻该拒绝谁」。
 *
 * 目前不含设备名称与平台信息：那些字段要由移动端在配对请求里上报，而移动端尚未实现
 * （plan §47 里的「device name」是移动端自己的设置项）。与其先占一个恒为空的字段，不如
 * 等真正的数据来源出现——界面上显示身份标识与配对时刻，已经足够让操作者认出「这台是我刚
 * 配的」，而一个永远空着的名称列只会让列表看起来像坏了。
 *
 * @property deviceIdentifier 插件分配的设备身份，同时是授权判断与审计追溯的主键。
 * @property pairedAt 配对成立的时刻。注意这是事件发生的时刻，而不是本条记录被写入内存的时刻。
 */
@Serializable
data class PairedDevice(
    val deviceIdentifier: DeviceIdentifier,
    @Serializable(with = EpochMillisecondsInstantSerializer::class)
    val pairedAt: Instant,
)
