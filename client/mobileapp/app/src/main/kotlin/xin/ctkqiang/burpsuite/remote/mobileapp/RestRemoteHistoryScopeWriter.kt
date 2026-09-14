package xin.ctkqiang.burpsuite.remote.mobileapp

import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.command.AddHistoryToScope
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.RemoteConnectionSettingsStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteFailure
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteHistoryScopeWriter

/**
 * 作用域写入端口的实现。
 *
 * 连接配置只存在本机偏好里，所以取值这一步必须发生在装配层：界面层看得见端口，看不见地址。
 *
 * 每次点击都取一个新的执行身份：加作用域本身幂等（同一主机写两次与写一次等价），所以重试换不换
 * 身份都不会产生第二次副作用；而每一次点击都是一次独立意图，各带各的身份，插件那边的执行台账
 * 才能如实记下「用户点了几下」（rules.md §5.5）。
 *
 * 没配过地址就直接给 [RemoteFailure.DeviceNotPaired]，一个字节都不发出去——向一个不存在的端点
 * 发请求只会让用户多等一次超时，再得到一句「连不上」，而真实原因是「你还没配对」。
 */
class RestRemoteHistoryScopeWriter(
    private val remoteControlClient: RemoteControlClient,
    private val connectionSettingsStore: RemoteConnectionSettingsStore,
    private val technicalLog: TechnicalLog,
) : RemoteHistoryScopeWriter {
    override suspend fun addHistoryHostToScope(historyIdentifier: String): RemoteResult<Unit> {
        val configuration = connectionSettingsStore.readConnectionConfiguration()
        if (configuration == null) {
            record(
                category = TechnicalLogCategory.Failure,
                message = "没有可用的连接配置，无法把主机加入作用域",
                attributes = mapOf("historyIdentifier" to historyIdentifier),
            )
            return RemoteResult.Failed(RemoteFailure.DeviceNotPaired)
        }

        val command =
            AddHistoryToScope(
                historyIdentifier = HistoryIdentifier(historyIdentifier),
                operationIdentifier = remoteControlClient.nextOperationIdentifier(),
            )
        val result = remoteControlClient.dispatch(configuration, command)
        reportOutcome(historyIdentifier = historyIdentifier, result = result)
        return result
    }

    private fun reportOutcome(
        historyIdentifier: String,
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
                    is RemoteResult.Succeeded -> "主机已加入作用域"
                    is RemoteResult.Failed -> "主机加入作用域失败"
                },
            attributes =
                when (result) {
                    is RemoteResult.Succeeded -> mapOf("historyIdentifier" to historyIdentifier)

                    is RemoteResult.Failed ->
                        mapOf(
                            "historyIdentifier" to historyIdentifier,
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
}
