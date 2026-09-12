package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.settings.RemoteConnectionSettingsStore

/**
 * 从连接配置存储里读设备身份。
 *
 * 传输层只认「已保存的身份是什么」，至于它被加密存在哪个文件里，是存储实现的事。
 */
class StoredDeviceIdentifierProvider(
    private val connectionSettingsStore: RemoteConnectionSettingsStore,
) : RemoteDeviceIdentifierProvider {
    override suspend fun currentDeviceIdentifier(): DeviceIdentifier? = connectionSettingsStore.readDeviceIdentifier()
}
