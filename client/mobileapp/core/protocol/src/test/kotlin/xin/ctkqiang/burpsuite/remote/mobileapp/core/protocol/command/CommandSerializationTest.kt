package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.OperationIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.ScreenshotIdentifier

class CommandSerializationTest {
    @Test
    fun `ConnectToBurp survives a serialization round trip`() {
        val command = ConnectToBurp(operationIdentifier = OPERATION_IDENTIFIER)

        assertEquals(command, roundTrip(ConnectToBurp.serializer(), command))
    }

    @Test
    fun `PairDevice survives a serialization round trip`() {
        val command =
            PairDevice(
                deviceIdentifier = DEVICE_IDENTIFIER,
                pairingChallengeIdentifier = PAIRING_CHALLENGE_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(PairDevice.serializer(), command))
    }

    @Test
    fun `ForwardIntercept survives a serialization round trip`() {
        val command =
            ForwardIntercept(
                interceptIdentifier = INTERCEPT_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(ForwardIntercept.serializer(), command))
    }

    @Test
    fun `DropIntercept survives a serialization round trip`() {
        val command =
            DropIntercept(
                interceptIdentifier = INTERCEPT_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(DropIntercept.serializer(), command))
    }

    @Test
    fun `ModifyIntercept survives a serialization round trip`() {
        val command =
            ModifyIntercept(
                interceptIdentifier = INTERCEPT_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(ModifyIntercept.serializer(), command))
    }

    @Test
    fun `SaveHistory survives a serialization round trip`() {
        val command =
            SaveHistory(
                historyIdentifier = HISTORY_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(SaveHistory.serializer(), command))
    }

    @Test
    fun `BookmarkHistory survives a serialization round trip`() {
        val command =
            BookmarkHistory(
                historyIdentifier = HISTORY_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(BookmarkHistory.serializer(), command))
    }

    @Test
    fun `AnnotateHistory survives a serialization round trip`() {
        val command =
            AnnotateHistory(
                historyIdentifier = HISTORY_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(AnnotateHistory.serializer(), command))
    }

    @Test
    fun `SendToRepeater survives a serialization round trip`() {
        val command =
            SendToRepeater(
                historyIdentifier = HISTORY_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(SendToRepeater.serializer(), command))
    }

    @Test
    fun `ExecuteRepeater survives a serialization round trip`() {
        val command =
            ExecuteRepeater(
                repeaterRequestIdentifier = REPEATER_REQUEST_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(ExecuteRepeater.serializer(), command))
    }

    @Test
    fun `ImportScreenshot survives a serialization round trip`() {
        val command =
            ImportScreenshot(
                screenshotIdentifier = SCREENSHOT_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(ImportScreenshot.serializer(), command))
    }

    @Test
    fun `BeautifyScreenshot survives a serialization round trip`() {
        val command =
            BeautifyScreenshot(
                screenshotIdentifier = SCREENSHOT_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(BeautifyScreenshot.serializer(), command))
    }

    @Test
    fun `ShareHistory survives a serialization round trip`() {
        val command =
            ShareHistory(
                historyIdentifier = HISTORY_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(ShareHistory.serializer(), command))
    }

    @Test
    fun `AddHistoryToScope survives a serialization round trip`() {
        val command =
            AddHistoryToScope(
                historyIdentifier = HISTORY_IDENTIFIER,
                operationIdentifier = OPERATION_IDENTIFIER,
            )

        assertEquals(command, roundTrip(AddHistoryToScope.serializer(), command))
    }

    // 编解码共用同一个 Json 实例：两边配置不同会掩盖契约问题
    private fun <T> roundTrip(
        serializer: KSerializer<T>,
        value: T,
    ): T = json.decodeFromString(serializer, json.encodeToString(serializer, value))

    private companion object {
        private val json = Json

        private val OPERATION_IDENTIFIER = OperationIdentifier("operation_01JXYZ")

        private val DEVICE_IDENTIFIER = DeviceIdentifier("device_01JABC")

        private val PAIRING_CHALLENGE_IDENTIFIER = PairingChallengeIdentifier("challenge_01JABC")

        private val INTERCEPT_IDENTIFIER = InterceptIdentifier("intercept_123")

        private val HISTORY_IDENTIFIER = HistoryIdentifier("history_123")

        private val REPEATER_REQUEST_IDENTIFIER = RepeaterRequestIdentifier("repeater_123")

        private val SCREENSHOT_IDENTIFIER = ScreenshotIdentifier("screenshot_123")
    }
}
