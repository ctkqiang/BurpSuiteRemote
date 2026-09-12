/**
 * Burp Remote —— 安全层 / 已配对设备登记处
 *
 * 登记此刻被允许执行控制命令的设备身份。它是「谁被信任」这一问题的唯一权威：界面上的设备
 * 列表读它，将来的授权判断也读它，因此不允许存在第二份副本——两份状态一旦不同步，
 * 被移除的设备就可能在另一条路径上继续通行。
 *
 * @author 钟智强
 */

package xin.ctkqiang.burpsuite.remote.security

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairedDevice
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 已配对设备的登记处。
 *
 * 目前状态只存在于内存中，随扩展卸载一同消失。这是当前阶段的自觉取舍：插件侧尚无持久化
 * 位置（plan §52 未定义存储层），而先落一份没有加密、没有迁移策略的明文文件，等于把
 * 「已配对设备」这一安全状态变成磁盘上可以随意编辑的一行文本。持久化要到凭据存储方案确定
 * 之后再补，届时的入口仍然是本类型，调用方无需改动。
 *
 * 并发语义：配对发生在（将来的）网络线程，读取与移除发生在界面线程，因此内部用并发映射而
 * 不是普通映射加锁。这里不需要「读快照与写」构成一个事务：界面读到的是某一瞬间的完整
 * 拷贝，即使同一时刻有设备被移除，读到的也只是「移除前」或「移除后」二者之一，不会读到
 * 一个半成品。
 *
 * @see PairedDevice
 */
class PairedDeviceRegistry {
    private val pairedDevices = ConcurrentHashMap<DeviceIdentifier, PairedDevice>()

    /**
     * 登记一台刚完成配对的设备。
     *
     * 同一身份重复登记会覆盖上一条记录，而不是并存两条：身份由插件分配，重复出现只意味着
     * 「同一台设备重新登记」，让两条记录同时存在会让「这台设备的配对时刻」变成一个没有答案
     * 的问题。
     *
     * @param deviceIdentifier 插件为该设备分配的身份。
     * @param pairedAt 配对成立的时刻。
     * @return 登记后的记录。
     */
    fun recordPairedDevice(
        deviceIdentifier: DeviceIdentifier,
        pairedAt: Instant,
    ): PairedDevice {
        val pairedDevice = PairedDevice(deviceIdentifier, pairedAt)
        pairedDevices[deviceIdentifier] = pairedDevice
        return pairedDevice
    }

    /**
     * 移除一台已配对设备，使它立即丧失身份。
     *
     * 移除是安全动作而非界面动作：调用方一旦拿到 true，就必须同时假定该设备的在线连接
     * （当传输层就绪后）也要被关闭，并且它携带的任何旧身份都必须被拒绝。设备若想重新获得
     * 授权，唯一路径是重新扫描一张新的配对票据——这正是「移除」被设计为不可逆的原因。
     *
     * @param deviceIdentifier 待移除的设备身份。
     * @return 该设备此前是否确实处于已配对状态。
     */
    fun removePairedDevice(deviceIdentifier: DeviceIdentifier): Boolean {
        val removedDevice = pairedDevices.remove(deviceIdentifier)
        return removedDevice != null
    }

    /**
     * 取当前已配对设备的快照。
     *
     * 返回的列表是拷贝，调用方对它做的任何改动都不会影响登记处。排序按配对时刻升序，
     * 时刻相同则按身份文本排序——同一毫秒内配对两台设备虽然罕见，但一个会随机变动的顺序
     * 会让「列表看起来自己跳了一下」，也让针对顺序的测试变成偶发失败。
     *
     * @return 按配对时刻升序排列的已配对设备列表；没有任何设备时返回空列表。
     */
    fun snapshot(): List<PairedDevice> {
        val orderedDevices =
            pairedDevices.values
                .sortedWith(compareBy({ it.pairedAt }, { it.deviceIdentifier.value }))
        return orderedDevices
    }
}
