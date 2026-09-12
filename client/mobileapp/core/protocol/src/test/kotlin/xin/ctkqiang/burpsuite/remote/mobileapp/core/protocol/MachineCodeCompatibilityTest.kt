package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.EventType

class MachineCodeCompatibilityTest {
    // 下面几组字面量逐个抄自插件侧的同名类型：任一端改了取值，都必须同时改这里，
    // 否则这道防漂移的闸门就形同虚设。
    private val extensionAggregateTypes =
        setOf("device", "history", "intercept", "repeater_request", "screenshot", "remote_session")

    private val extensionRejectionReasons =
        setOf(
            "device_not_paired",
            "device_not_authorized",
            "rate_limit_exceeded",
            "target_not_available",
            "unsupported_protocol_version",
            "pairing_session_not_available",
            "pairing_session_expired",
            "pairing_code_mismatch",
            "missing_operation_identifier",
        )

    private val extensionRemoteErrorCodes =
        setOf(
            "internal_failure",
            "burp_runtime_failure",
            "serialization_failure",
            "timeout",
            "not_implemented",
            "request_payload_too_large",
        )

    private val extensionCommandResults = setOf("succeeded", "rejected", "failed")

    @Test
    fun `aggregate types match the extension value set exactly`() {
        assertWireValueSet(AggregateType.serializer(), extensionAggregateTypes)
    }

    @Test
    fun `rejection reasons match the extension value set exactly`() {
        assertWireValueSet(RejectionReason.serializer(), extensionRejectionReasons)
    }

    @Test
    fun `remote error codes match the extension value set exactly`() {
        assertWireValueSet(RemoteErrorCode.serializer(), extensionRemoteErrorCodes)
    }

    @Test
    fun `command result variants match the extension value set exactly`() {
        assertExactSet(commandResultWireValues(), extensionCommandResults)
    }

    @Test
    fun `event types keep the extension dotted wire values`() {
        val wireValues =
            mapOf(
                EventType.DEVICE_CONNECTED to "device.connected",
                EventType.DEVICE_DISCONNECTED to "device.disconnected",
                EventType.DEVICE_PAIRED to "device.paired",
                EventType.DEVICE_UNPAIRED to "device.unpaired",
                EventType.HISTORY_ITEM_OBSERVED to "history.item.observed",
                EventType.HISTORY_ITEM_SAVED to "history.item.saved",
                EventType.HISTORY_ANNOTATION_ADDED to "history.annotation.added",
                EventType.INTERCEPT_CREATED to "intercept.created",
                EventType.INTERCEPT_MODIFIED to "intercept.modified",
                EventType.INTERCEPT_FORWARDED to "intercept.forwarded",
                EventType.INTERCEPT_DROPPED to "intercept.dropped",
                EventType.REPEATER_CREATED to "repeater.created",
                EventType.REPEATER_EXECUTION_STARTED to "repeater.execution.started",
                EventType.REPEATER_EXECUTION_COMPLETED to "repeater.execution.completed",
                EventType.SCREENSHOT_IMPORTED to "screenshot.imported",
                EventType.SCREENSHOT_BEAUTIFIED to "screenshot.beautified",
            )

        wireValues.forEach { (eventType, wireValue) ->
            assertEquals(wireValue, eventType.value)
        }
    }

    @Test
    fun `the default port and the protocol version match the extension`() {
        assertEquals(9000, DEFAULT_REMOTE_PORT)
        assertEquals(1, RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION)
    }

    private fun <T> assertWireValueSet(
        serializer: KSerializer<T>,
        extensionWireValues: Set<String>,
    ) {
        assertExactSet(serializer.descriptor.elementNames.toSet(), extensionWireValues)

        // 再逐字解码编回：字面量对不上时这里会直接抛序列化异常，而不是悄悄放过
        extensionWireValues.forEach { wireValue ->
            val decoded = json.decodeFromJsonElement(serializer, JsonPrimitive(wireValue))

            assertEquals(wireValue, encodedValueOf(serializer, decoded))
        }
    }

    // 封闭接口的描述符只有 type/value 两个槽位，列不出变体；wire 字面量落在子类的 @SerialName 上，
    // 只能顺着密封层级的嵌套类，去各子类 Companion 的序列化器描述符里取 serialName。
    private fun commandResultWireValues(): Set<String> =
        CommandResult::class.java.declaredClasses
            .filter { CommandResult::class.java.isAssignableFrom(it) }
            .mapTo(mutableSetOf()) { variant ->
                val companion = variant.getField("Companion").get(null)
                val serializer = companion.javaClass.getMethod("serializer").invoke(companion) as KSerializer<*>

                serializer.descriptor.serialName
            }

    // 集合必须全等而非包含：多了会解出意外分支，少了会静默退回，两种漂移都要在失败信息里点名
    private fun assertExactSet(
        clientWireValues: Set<String>,
        extensionWireValues: Set<String>,
    ) {
        val onlyOnClient = clientWireValues - extensionWireValues
        val onlyOnExtension = extensionWireValues - clientWireValues

        assertEquals(
            extensionWireValues,
            clientWireValues,
            "客户端多出 $onlyOnClient；客户端缺少 $onlyOnExtension",
        )
    }

    private fun <T> encodedValueOf(
        serializer: KSerializer<T>,
        value: T,
    ): String = json.encodeToJsonElement(serializer, value).jsonPrimitive.content

    private companion object {
        private val json = Json
    }
}
