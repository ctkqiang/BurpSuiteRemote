package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier

class CommandResultSerializationTest {
    @Test
    fun `a succeeded result survives a serialization round trip`() {
        val result: CommandResult = CommandResult.Succeeded(OperationIdentifier("operation_01JXYZ"))

        assertEquals(result, roundTrip(CommandResult.serializer(), result))
    }

    @Test
    fun `a rejected result survives a serialization round trip`() {
        val result: CommandResult = CommandResult.Rejected(RejectionReason.DeviceNotPaired)

        assertEquals(result, roundTrip(CommandResult.serializer(), result))
    }

    @Test
    fun `a failed result survives a serialization round trip`() {
        val result: CommandResult = CommandResult.Failed(RemoteError(RemoteErrorCode.Timeout, isRetryable = true))

        assertEquals(result, roundTrip(CommandResult.serializer(), result))
    }

    // 编解码共用同一个 Json 实例：两边配置不同会掩盖契约问题
    private fun <T> roundTrip(
        serializer: KSerializer<T>,
        value: T,
    ): T = json.decodeFromString(serializer, json.encodeToString(serializer, value))

    private companion object {
        private val json = Json
    }
}
