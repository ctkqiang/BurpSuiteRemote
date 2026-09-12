package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.Serializable

/**
 * /v1/capabilities 的载荷形状。
 *
 * @property capabilities 插件自报的能力名。
 */
@Serializable
data class ServerCapabilitiesPayload(val capabilities: List<String>)
