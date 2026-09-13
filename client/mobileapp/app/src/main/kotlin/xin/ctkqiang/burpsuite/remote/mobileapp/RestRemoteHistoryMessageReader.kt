package xin.ctkqiang.burpsuite.remote.mobileapp

import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.RemoteConnectionSettingsStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteHistoryMessage
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteHistoryMessageReader

/**
 * 报文本体读取端口的实现。
 *
 * 连接配置只存在本机偏好里，所以取值这一步必须发生在装配层：界面层看得见端口，看不见地址。
 *
 * 没配过地址就直接给 [RemoteFailure.DeviceNotPaired]，一个字节都不发出去——向一个不存在的端点
 * 发请求只会让用户多等一次超时，再得到一句「连不上」，而真实原因是「你还没配对」。
 */
class RestRemoteHistoryMessageReader(
    private val remoteControlClient: RemoteControlClient,
    private val connectionSettingsStore: RemoteConnectionSettingsStore,
    private val technicalLog: TechnicalLog,
) : RemoteHistoryMessageReader {
    override suspend fun readHistoryMessage(historyIdentifier: String): RemoteResult<RemoteHistoryMessage> {
        val configuration = connectionSettingsStore.readConnectionConfiguration()
        if (configuration == null) {
            technicalLog.record(
                TechnicalLogEvent(
                    category = TechnicalLogCategory.Failure,
                    message = "没有可用的连接配置，无法读取报文本体",
                    attributes = mapOf("historyIdentifier" to historyIdentifier),
                ),
            )
            return RemoteResult.Failed(RemoteFailure.DeviceNotPaired)
        }

        val result = remoteControlClient.readRemoteHistoryMessage(configuration, HistoryIdentifier(historyIdentifier))
        reportRead(historyIdentifier = historyIdentifier, result = result)
        return result
    }

    private fun reportRead(
        historyIdentifier: String,
        result: RemoteResult<RemoteHistoryMessage>,
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
                    is RemoteResult.Succeeded -> "报文本体已取回"
                    is RemoteResult.Failed -> "报文本体读取失败"
                },
            attributes =
                when (result) {
                    is RemoteResult.Succeeded -> {
                        mapOf(
                            "historyIdentifier" to historyIdentifier,
                            "requestBodyLength" to (result.value.requestBody?.length ?: 0).toString(),
                            "responseBodyLength" to (result.value.responseBody?.length ?: 0).toString(),
                        )
                    }

                    is RemoteResult.Failed -> {
                        mapOf(
                            "historyIdentifier" to historyIdentifier,
                            "failure" to result.failure::class.simpleName.orEmpty(),
                        )
                    }
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
}
