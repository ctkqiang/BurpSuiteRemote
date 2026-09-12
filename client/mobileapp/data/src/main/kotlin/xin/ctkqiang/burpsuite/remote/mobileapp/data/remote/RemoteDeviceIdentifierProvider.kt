package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.DeviceIdentifier

/**
 * 已保存设备身份的读取端口。
 *
 * 身份是配对成功后插件签发的凭据，属于机密（rules.md §12）：客户端这一层只读取它，不决定它存在哪；
 * 把它放进明文文件假装完成了持久化，比留一个缺口更糟。
 */
fun interface RemoteDeviceIdentifierProvider {
    /** 读取已保存的设备身份；尚未配对或读取不到时返回 null。 */
    suspend fun currentDeviceIdentifier(): DeviceIdentifier?
}
