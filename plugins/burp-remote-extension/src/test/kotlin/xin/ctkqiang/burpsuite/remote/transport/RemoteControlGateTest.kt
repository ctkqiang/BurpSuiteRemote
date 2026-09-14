/**
 * 安全闸门测试：未配对拒绝、缺操作标识拒绝、幂等只执行一次、限流触发，以及重试不被限流挡住。
 * 时钟与限流参数都是注入的，用例不碰网络、不等真实时间。
 */

package xin.ctkqiang.burpsuite.remote.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.CommandResult
import xin.ctkqiang.burpsuite.remote.protocol.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.protocol.RejectionReason
import xin.ctkqiang.burpsuite.remote.protocol.RemoteError
import xin.ctkqiang.burpsuite.remote.protocol.RemoteErrorCode
import xin.ctkqiang.burpsuite.remote.security.PairedDeviceRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RemoteControlGateTest {
    @Test
    fun `a control command from an unpaired device is rejected as device not paired`() {
        val gate = createGate(pairedDeviceRegistry = PairedDeviceRegistry())

        val commandResult =
            gate.handleControlCommand(
                DEVICE_IDENTIFIER,
                OPERATION_IDENTIFIER,
                COMMAND_TYPE,
                { notImplementedResult() },
            )

        assertEquals(CommandResult.Rejected(RejectionReason.DeviceNotPaired), commandResult)
    }

    @Test
    fun `a control command without an operation identifier is rejected`() {
        val gate = createGate()

        val commandResult = gate.handleControlCommand(DEVICE_IDENTIFIER, null, COMMAND_TYPE) { notImplementedResult() }

        assertEquals(CommandResult.Rejected(RejectionReason.MissingOperationIdentifier), commandResult)
    }

    @Test
    fun `repeating an operation identifier returns the first result and executes once`() {
        val gate = createGate()
        var executionCount = 0

        val firstResult =
            gate.handleControlCommand(DEVICE_IDENTIFIER, OPERATION_IDENTIFIER, COMMAND_TYPE) {
                executionCount += 1
                notImplementedResult()
            }
        val repeatedResult =
            gate.handleControlCommand(DEVICE_IDENTIFIER, OPERATION_IDENTIFIER, COMMAND_TYPE) {
                executionCount += 1
                notImplementedResult()
            }

        assertEquals(firstResult, repeatedResult)
        assertEquals(1, executionCount)
    }

    @Test
    fun `the execution action receives the operation identifier it must answer with`() {
        val gate = createGate()
        var receivedOperationIdentifier: OperationIdentifier? = null

        gate.handleControlCommand(DEVICE_IDENTIFIER, OPERATION_IDENTIFIER, COMMAND_TYPE) { operationIdentifier ->
            receivedOperationIdentifier = operationIdentifier
            notImplementedResult()
        }

        assertEquals(OPERATION_IDENTIFIER, receivedOperationIdentifier)
    }

    @Test
    fun `exceeding the burst capacity is rejected as rate limited`() {
        val gate = createGate(bucketCapacity = 2)

        val firstResult =
            gate.handleControlCommand(
                DEVICE_IDENTIFIER,
                FIRST_OPERATION_IDENTIFIER,
                COMMAND_TYPE,
                { notImplementedResult() },
            )
        val secondResult =
            gate.handleControlCommand(
                DEVICE_IDENTIFIER,
                SECOND_OPERATION_IDENTIFIER,
                COMMAND_TYPE,
                { notImplementedResult() },
            )
        val thirdResult =
            gate.handleControlCommand(
                DEVICE_IDENTIFIER,
                THIRD_OPERATION_IDENTIFIER,
                COMMAND_TYPE,
                { notImplementedResult() },
            )

        assertTrue(firstResult is CommandResult.Failed)
        assertTrue(secondResult is CommandResult.Failed)
        assertEquals(CommandResult.Rejected(RejectionReason.RateLimitExceeded), thirdResult)
    }

    @Test
    fun `a repeated operation is answered from the log even when the burst is exhausted`() {
        val gate = createGate(bucketCapacity = 1)

        val firstResult =
            gate.handleControlCommand(
                DEVICE_IDENTIFIER,
                OPERATION_IDENTIFIER,
                COMMAND_TYPE,
                { notImplementedResult() },
            )
        val repeatedResult =
            gate.handleControlCommand(
                DEVICE_IDENTIFIER,
                OPERATION_IDENTIFIER,
                COMMAND_TYPE,
                { notImplementedResult() },
            )

        assertEquals(firstResult, repeatedResult)
    }

    private fun createGate(
        pairedDeviceRegistry: PairedDeviceRegistry = registryWithPairedDevice(),
        bucketCapacity: Int = DEFAULT_BUCKET_CAPACITY,
    ): RemoteControlGate =
        RemoteControlGate(
            pairedDeviceRegistry = pairedDeviceRegistry,
            operationLog = InMemoryRemoteOperationLog(),
            // 补充速率为零：限流用例因此不依赖时间流逝，超出容量必然被拒。
            rateLimiter =
                RemoteDeviceRateLimiter(
                    clock = Clock.fixed(TEST_INSTANT, ZoneOffset.UTC),
                    bucketCapacity = bucketCapacity,
                    refillTokensPerSecond = 0.0,
                ),
            auditLogger = RemoteAuditLogger(auditSink = {}),
        )

    private fun registryWithPairedDevice(): PairedDeviceRegistry =
        PairedDeviceRegistry().apply { recordPairedDevice(DEVICE_IDENTIFIER, TEST_INSTANT) }

    private fun notImplementedResult(): CommandResult =
        CommandResult.Failed(RemoteError(RemoteErrorCode.NotImplemented, isRetryable = false))

    private companion object {
        private val DEVICE_IDENTIFIER = DeviceIdentifier("device_0000000000000001")

        private val OPERATION_IDENTIFIER = OperationIdentifier("operation_0000000000000001")

        private val FIRST_OPERATION_IDENTIFIER = OperationIdentifier("operation_0000000000000002")

        private val SECOND_OPERATION_IDENTIFIER = OperationIdentifier("operation_0000000000000003")

        private val THIRD_OPERATION_IDENTIFIER = OperationIdentifier("operation_0000000000000004")

        private const val COMMAND_TYPE = "intercept.forward"

        private const val DEFAULT_BUCKET_CAPACITY = 20

        private val TEST_INSTANT: Instant = Instant.parse("2026-01-01T00:00:00Z")
    }
}
