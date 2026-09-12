package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import kotlinx.serialization.Serializable

/**
 * 清单里的一条（plan §26）。
 *
 * @property historyIdentifier 历史记录标识。
 * @property method HTTP 方法。
 * @property host 目标主机。
 * @property path 请求路径。
 */
@Serializable
data class ExportManifestEntry(
    val historyIdentifier: String,
    val method: String?,
    val host: String?,
    val path: String?,
)
