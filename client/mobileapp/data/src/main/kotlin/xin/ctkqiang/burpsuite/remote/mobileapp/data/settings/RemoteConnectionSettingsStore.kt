package xin.ctkqiang.burpsuite.remote.mobileapp.data.settings

import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.RemoteServerEndpoint

/**
 * 连接配置与设备身份的读写端口。
 *
 * 声明在数据层而不是领域层：领域层只关心「往哪连」，而「这些值存在哪、身份要不要加密」
 * 是适配器的事；把存储细节提到领域层，只会让领域层开始关心 Android 的密钥库。
 *
 * 地址端口与设备身份分开读写，是因为它们被清除的时机不同：换台机器连要改地址，重新配对才要换身份。
 *
 * 设备身份是插件签发的凭据（plan §55），因此它的读取实现必须自行处理加密，明文落盘的实现不算实现。
 */
interface RemoteConnectionSettingsStore {
    /** 观察上次用过的连接配置；从未配置过时为 null，界面据此显示「尚未配置」。 */
    fun observeConnectionConfiguration(): Flow<RemoteConnectionConfiguration?>

    /** 读上次用过的连接配置；从未配置过时返回 null。 */
    suspend fun readConnectionConfiguration(): RemoteConnectionConfiguration?

    /** 记下这次用过的连接配置。 */
    suspend fun saveConnectionConfiguration(configuration: RemoteConnectionConfiguration)

    /** 只改地址与端口：设备名与 TLS 方案是另外两回事，不该被顺手覆盖。 */
    suspend fun saveServerEndpoint(endpoint: RemoteServerEndpoint)

    /** 抹掉地址与端口；设备身份不动。 */
    suspend fun clearServerEndpoint()

    /** 观察本机保存的设备身份；尚未配对时为 null。 */
    fun observeDeviceIdentifier(): Flow<DeviceIdentifier?>

    /** 读已保存的设备身份；尚未配对或凭据已不可解读时返回 null。 */
    suspend fun readDeviceIdentifier(): DeviceIdentifier?

    /** 保存插件签发的设备身份。 */
    suspend fun saveDeviceIdentifier(deviceIdentifier: DeviceIdentifier)

    /** 抹掉本机保存的设备身份；下次连接必须重新配对。 */
    suspend fun clearDeviceIdentifier()
}
