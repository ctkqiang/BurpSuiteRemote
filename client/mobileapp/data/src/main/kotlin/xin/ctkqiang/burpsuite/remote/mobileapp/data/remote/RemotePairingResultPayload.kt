package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.Serializable

/**
 * /v1/pair 成功时的载荷形状；插件只回身份本身，不回配对凭据。
 *
 * @property deviceIdentifier 插件签发的设备身份。
 */
@Serializable
data class RemotePairingResultPayload(val deviceIdentifier: String)
