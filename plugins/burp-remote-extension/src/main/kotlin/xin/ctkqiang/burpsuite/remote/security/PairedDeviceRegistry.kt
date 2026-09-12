// 已配对设备登记处：谁被允许执行控制命令，在这里登记。

package xin.ctkqiang.burpsuite.remote.security

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairedDevice
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 已配对设备的登记处。
 *
 * 状态只在内存、随卸载消失：还没有可用的存储层，先落一份明文文件等于把授权状态变成磁盘上能随手改的一行。
 * 配对与读取分处不同线程，所以用并发映射而不是加锁。
 */
class PairedDeviceRegistry {
    private val pairedDevices = ConcurrentHashMap<DeviceIdentifier, PairedDevice>()

    /**
     * 登记一台刚完成配对的设备。
     *
     * 同一身份重复登记覆盖旧记录：身份由插件分配，重复只代表同一台设备重新登记，并存会让配对时刻没有答案。
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
     * 这是安全动作：拿到 true 就要假定它的连接会被断开、旧身份会被拒绝；重新授权只能靠重扫一张新票据。
     */
    fun removePairedDevice(deviceIdentifier: DeviceIdentifier): Boolean {
        val removedDevice = pairedDevices.remove(deviceIdentifier)
        return removedDevice != null
    }

    /**
     * 取当前已配对设备的快照。
     *
     * 返回的是拷贝，改动不影响登记处；排序保证顺序稳定，否则列表会自己跳一下，顺序相关的测试也会偶发失败。
     */
    fun snapshot(): List<PairedDevice> {
        val orderedDevices =
            pairedDevices.values
                .sortedWith(compareBy({ it.pairedAt }, { it.deviceIdentifier.value }))
        return orderedDevices
    }
}
