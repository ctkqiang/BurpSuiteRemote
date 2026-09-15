package xin.ctkqiang.burpsuite.remote.mobileapp

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.SendToRepeater
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.RemoteConnectionSettingsStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteRepeaterWriter

/**
 * Repeater 写入端口的实现。
 *
 * 连接配置只存在本机偏好里，所以取值这一步必须发生在装配层：界面层看得见端口，看不见地址。
 *
 * 每次点击都取一个新的执行身份：Repeater create 在插件侧非幂等（每次都建一条新条目），
 * 因此重试时换一个新身份才能让插件区分「这是第二次尝试」与「这是同一份命令的重发」。
 *
 * 没配过地址就直接给 [RemoteFailure.DeviceNotPaired]，一个字节都不发出去。
 *
 * 请求体只放插件端 `handleRepeaterCreate()` 要读的两个字段：`requestText` 与 `tabName`。
 * 命令对象（[SendToRepeater]）仍然携带 `historyIdentifier` 与 `operationIdentifier`，
 * 前者用于本地日志，后者通过请求头上行——两者都不进请求体，因为插件不读它们。
 */
class RestRemoteRepeaterWriter(
    private val remoteControlClient: RemoteControlClient,
    private val connectionSettingsStore: RemoteConnectionSettingsStore,
    private val technicalLog: TechnicalLog,
) : RemoteRepeaterWriter {
    override suspend fun sendToRepeater(
        requestText: String,
        tabName: String?,
    ): RemoteResult<Unit> {
        val configuration = connectionSettingsStore.readConnectionConfiguration()
        if (configuration == null) {
            record(
                category = TechnicalLogCategory.Failure,
                message = "没有可用的连接配置，无法推送到 Repeater",
                attributes = mapOf("requestTextLength" to requestText.length.toString()),
            )
            return RemoteResult.Failed(RemoteFailure.DeviceNotPaired)
        }

        val operationIdentifier = remoteControlClient.nextOperationIdentifier()
        val command =
            SendToRepeater(
                historyIdentifier = HistoryIdentifier(PLACEHOLDER_HISTORY_IDENTIFIER),
                operationIdentifier = operationIdentifier,
            )
        val body = buildRepeaterRequestBody(requestText, tabName)
        val result = remoteControlClient.dispatchWithBody(configuration, command, body)
        reportOutcome(requestTextLength = requestText.length, result = result)
        return result
    }

    /**
     * 构建插件端 `handleRepeaterCreate()` 要读的 JSON 请求体。
     *
     * 字段名与插件端 `RemoteHttpServer` 的 `REQUEST_TEXT_FIELD` / `TAB_NAME_FIELD` 常量逐一对齐：
     * `requestText` 和 `tabName`。多余字段插件不读，因此只放这两条。
     */
    private fun buildRepeaterRequestBody(
        requestText: String,
        tabName: String?,
    ): String =
        buildJsonObject {
            put("requestText", requestText)
            if (tabName != null) {
                put("tabName", tabName)
            }
        }.toString()

    private fun reportOutcome(
        requestTextLength: Int,
        result: RemoteResult<Unit>,
    ) {
        record(
            category =
                if (result is RemoteResult.Succeeded) {
                    TechnicalLogCategory.Transport
                } else {
                    TechnicalLogCategory.Failure
                },
            message =
                when (result) {
                    is RemoteResult.Succeeded -> "请求已推送到 Repeater"
                    is RemoteResult.Failed -> "推送到 Repeater 失败"
                },
            attributes =
                when (result) {
                    is RemoteResult.Succeeded ->
                        mapOf("requestTextLength" to requestTextLength.toString())

                    is RemoteResult.Failed ->
                        mapOf(
                            "requestTextLength" to requestTextLength.toString(),
                            "failure" to result.failure::class.simpleName.orEmpty(),
                        )
                },
        )
    }

    private fun record(
        category: TechnicalLogCategory,
        message: String,
        attributes: Map<String, String>,
    ) {
        technicalLog.record(TechnicalLogEvent(category = category, message = message, attributes = attributes))
    }

    private companion object {
        // SendToRepeater 命令仍然要求一个 historyIdentifier，但插件端 Repeater create endpoint
        // 不读它——这里放一个占位值，只为满足命令对象的构造约束。将来命令去掉这个字段时一并清理。
        const val PLACEHOLDER_HISTORY_IDENTIFIER = "from_history_detail"
    }
}
