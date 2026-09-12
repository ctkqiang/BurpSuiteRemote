// 已连接设备的登记表。

package xin.ctkqiang.burpsuite.remote.transport

import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 事件通道的连接登记表（plan §54 的连接数上限在这里生效）。
 *
 * 名额与设备身份分开计数：认证之前还没有身份，但未认证的洪泛同样要能被挡住。
 * 结论是「一台设备一条活动连接」：同一身份再次登记会顶掉旧关联，界面与状态接口因此只有一个答案。
 */
class RemoteConnectionRegistry(
    private val maximumConnectionCount: Int = DEFAULT_MAXIMUM_CONNECTION_COUNT,
) {
    private val openConnectionCount = AtomicInteger(0)

    private val connectedDevices = ConcurrentHashMap<DeviceIdentifier, Unit>()

    /** 占一个连接名额；满员时返回 false，调用方必须立即断开这条连接。 */
    fun openConnection(): Boolean {
        while (true) {
            val currentCount = openConnectionCount.get()
            if (currentCount >= maximumConnectionCount) {
                return false
            }
            if (openConnectionCount.compareAndSet(currentCount, currentCount + 1)) {
                return true
            }
        }
    }

    /** 归还连接名额；必须与 [openConnection] 成对调用，漏掉一次就永久少一个名额。 */
    fun closeConnection() {
        openConnectionCount.decrementAndGet()
    }

    /** 记下这条连接属于哪台设备，状态接口据此报告在线的设备。 */
    fun associateDevice(deviceIdentifier: DeviceIdentifier) {
        connectedDevices[deviceIdentifier] = Unit
    }

    /** 撤销设备关联；连接断开时调用。 */
    fun disassociateDevice(deviceIdentifier: DeviceIdentifier) {
        connectedDevices.remove(deviceIdentifier)
    }

    /** 当前仍有活动连接的设备数量。 */
    fun connectedDeviceCount(): Int = connectedDevices.size

    /** 当前仍有活动连接的设备快照，按身份排序以保证输出稳定。 */
    fun snapshot(): List<DeviceIdentifier> = connectedDevices.keys.sortedBy { it.value }

    private companion object {
        // 连接数上限原文未定义，此处选定值：最多 4 条并发事件通道。
        // 依据：一台手机通常只开一条，留出重连与多设备调试的余量；再多也只是徒增并发压力。
        private const val DEFAULT_MAXIMUM_CONNECTION_COUNT = 4
    }
}
