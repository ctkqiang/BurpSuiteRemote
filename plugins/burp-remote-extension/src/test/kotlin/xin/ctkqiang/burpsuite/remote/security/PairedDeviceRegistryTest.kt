/**
 * 已配对设备登记处测试：登记/移除/覆盖/排序的语义，以及快照是拷贝而非视图。
 * 界面显示的列表是否等于真实授权状态，全看这几条性质。
 */

package xin.ctkqiang.burpsuite.remote.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.PairedDevice
import java.time.Instant

class PairedDeviceRegistryTest {
    private val pairedDeviceRegistry = PairedDeviceRegistry()

    @Test
    fun `a recorded device appears in the snapshot`() {
        pairedDeviceRegistry.recordPairedDevice(DEVICE_IDENTIFIER, PAIRED_AT)

        assertEquals(listOf(PairedDevice(DEVICE_IDENTIFIER, PAIRED_AT)), pairedDeviceRegistry.snapshot())
    }

    @Test
    fun `removing a paired device takes it out of the snapshot`() {
        pairedDeviceRegistry.recordPairedDevice(DEVICE_IDENTIFIER, PAIRED_AT)

        assertTrue(pairedDeviceRegistry.removePairedDevice(DEVICE_IDENTIFIER))
        assertTrue(pairedDeviceRegistry.snapshot().isEmpty())
    }

    @Test
    fun `removing a device that is not paired reports that nothing was removed`() {
        assertFalse(pairedDeviceRegistry.removePairedDevice(DEVICE_IDENTIFIER))
    }

    @Test
    fun `the snapshot is ordered by the pairing time`() {
        pairedDeviceRegistry.recordPairedDevice(LATER_DEVICE_IDENTIFIER, LATER_INSTANT)
        pairedDeviceRegistry.recordPairedDevice(DEVICE_IDENTIFIER, PAIRED_AT)

        assertEquals(
            listOf(DEVICE_IDENTIFIER, LATER_DEVICE_IDENTIFIER),
            pairedDeviceRegistry.snapshot().map { pairedDevice -> pairedDevice.deviceIdentifier },
        )
    }

    @Test
    fun `recording the same identity again replaces the previous record`() {
        pairedDeviceRegistry.recordPairedDevice(DEVICE_IDENTIFIER, PAIRED_AT)
        pairedDeviceRegistry.recordPairedDevice(DEVICE_IDENTIFIER, LATER_INSTANT)

        assertEquals(listOf(PairedDevice(DEVICE_IDENTIFIER, LATER_INSTANT)), pairedDeviceRegistry.snapshot())
    }

    @Test
    fun `a snapshot taken earlier is not affected by a later recording`() {
        val snapshotBeforeRecording = pairedDeviceRegistry.snapshot()

        pairedDeviceRegistry.recordPairedDevice(DEVICE_IDENTIFIER, PAIRED_AT)

        assertTrue(snapshotBeforeRecording.isEmpty())
        assertEquals(1, pairedDeviceRegistry.snapshot().size)
    }

    private companion object {
        private val DEVICE_IDENTIFIER = DeviceIdentifier("device_0000000000000001")

        private val LATER_DEVICE_IDENTIFIER = DeviceIdentifier("device_0000000000000002")

        private val PAIRED_AT: Instant = Instant.parse("2026-01-01T00:00:00Z")

        private val LATER_INSTANT: Instant = Instant.parse("2026-01-01T00:01:00Z")
    }
}
