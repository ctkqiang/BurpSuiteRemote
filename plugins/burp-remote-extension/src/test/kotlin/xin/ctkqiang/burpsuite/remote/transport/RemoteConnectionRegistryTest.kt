/**
 * 连接登记表测试：名额上限、名额归还与设备关联的语义。
 * 上限决定「第几条连接会被拒绝」，归还决定「断开后能不能重新连上」。
 */

package xin.ctkqiang.burpsuite.remote.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier

class RemoteConnectionRegistryTest {
    @Test
    fun `opening connections up to the limit succeeds and one more is refused`() {
        val registry = RemoteConnectionRegistry(maximumConnectionCount = CONNECTION_LIMIT)

        repeat(CONNECTION_LIMIT) { assertTrue(registry.openConnection()) }

        assertFalse(registry.openConnection())
    }

    @Test
    fun `closing a connection frees a slot`() {
        val registry = RemoteConnectionRegistry(maximumConnectionCount = 1)
        assertTrue(registry.openConnection())

        registry.closeConnection()

        assertTrue(registry.openConnection())
    }

    @Test
    fun `an associated device appears in the snapshot`() {
        val registry = RemoteConnectionRegistry()
        registry.openConnection()

        registry.associateDevice(DEVICE_IDENTIFIER)

        assertEquals(listOf(DEVICE_IDENTIFIER), registry.snapshot())
        assertEquals(1, registry.connectedDeviceCount())
    }

    @Test
    fun `disassociating a device removes it from the snapshot`() {
        val registry = RemoteConnectionRegistry()
        registry.associateDevice(DEVICE_IDENTIFIER)

        registry.disassociateDevice(DEVICE_IDENTIFIER)

        assertTrue(registry.snapshot().isEmpty())
    }

    private companion object {
        private const val CONNECTION_LIMIT = 2

        private val DEVICE_IDENTIFIER = DeviceIdentifier("device_0000000000000001")
    }
}
