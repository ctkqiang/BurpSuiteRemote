package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogSeverity
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.RemoteConnectionSettingsStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.PairingAttempt
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult

/**
 * 把「配对成功后要落地什么」与「连接挂在哪个作用域」补上的远程控制端口。
 *
 * 传输实现只管一次会话；这两件事属于装配决策：不落地身份，重启就得重扫；连接挂在界面作用域，
 * 转屏就会断线。两者都放在这里，界面与传输层都不必知道。
 */
class PersistingRemoteControlClient(
    private val delegate: RemoteControlClient,
    private val connectionSettingsStore: RemoteConnectionSettingsStore,
    private val connectionScope: CoroutineScope,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) : RemoteControlClient by delegate {
    override suspend fun connect(configuration: RemoteConnectionConfiguration) {
        connectionSettingsStore.saveConnectionConfiguration(configuration)
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.Transport,
                message = "开始建立事件流连接",
                attributes = mapOf("host" to configuration.host, "port" to configuration.port.toString()),
            ),
        )
        // 连接挂在进程级作用域：放进调用方的作用域，转屏会把整条会话连根拔掉。
        connectionScope.launch { delegate.connect(configuration) }.join()
    }

    override suspend fun pair(pairingAttempt: PairingAttempt): RemoteResult<DeviceIdentifier> {
        // 配对码与挑战标识都不进日志：前者是一次性凭据，后者能被用来抢答配对话（rules.md §12）。
        technicalLog.record(
            TechnicalLogEvent(
                category = TechnicalLogCategory.Pairing,
                message = "向插件发起配对",
                attributes = mapOf("host" to pairingAttempt.host, "port" to pairingAttempt.port.toString()),
            ),
        )

        return when (val result = delegate.pair(pairingAttempt)) {
            is RemoteResult.Succeeded -> {
                val configuration =
                    RemoteConnectionConfiguration(
                        host = pairingAttempt.host,
                        port = pairingAttempt.port,
                        deviceName = Build.MODEL.orEmpty(),
                        isTlsEnabled = pairingAttempt.isTlsEnabled,
                    )
                connectionSettingsStore.saveConnectionConfiguration(configuration)
                persistDeviceIdentity(deviceIdentifier = result.value)
                technicalLog.record(
                    TechnicalLogEvent(
                        category = TechnicalLogCategory.Pairing,
                        message = "配对成功，设备身份与地址已保存",
                        attributes = mapOf("host" to configuration.host, "port" to configuration.port.toString()),
                    ),
                )
                // 扫码的意图就是「连上」：配对成功立刻建连，不再要求用户按第二次。
                connectionScope.launch { delegate.connect(configuration) }
                result
            }

            is RemoteResult.Failed -> {
                technicalLog.record(
                    TechnicalLogEvent(
                        category = TechnicalLogCategory.Failure,
                        message = "配对被插件拒绝",
                        attributes = mapOf("failure" to result.failure.toString()),
                    ),
                )
                result
            }
        }
    }

    /**
     * 把设备身份落盘。
     *
     * 身份写不下去不该让配对流程炸掉：密钥库在个别设备上会拒绝建钥（用户改过锁屏、厂商实现缺陷），
     * 而配对本身已经成功了。如实记一条错误日志，本次会话照常可用，代价只是重启后要重新配对。
     */
    private suspend fun persistDeviceIdentity(deviceIdentifier: DeviceIdentifier) {
        try {
            connectionSettingsStore.saveDeviceIdentifier(deviceIdentifier)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (unpersistableIdentity: Exception) {
            technicalLog.record(
                TechnicalLogEvent(
                    category = TechnicalLogCategory.Failure,
                    message = "设备身份写不进密钥库，本次配对仅对当前会话有效",
                    severity = TechnicalLogSeverity.Error,
                    failure = unpersistableIdentity,
                ),
            )
        }
    }
}
