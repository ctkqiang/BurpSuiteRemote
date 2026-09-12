package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import kotlinx.serialization.Serializable

/**
 * 导出清单（plan §26）。
 *
 * 字段名是格式契约：导入方照着它校验，改了就等于换了格式，必须同步抬 [exportFormatVersion]。
 *
 * @property format 固定为 `burpremote`。
 * @property exportFormatVersion 导出格式版本。
 * @property createdAt 导出时刻，ISO-8601 文本。
 * @property items 这次导出包含的条目。
 */
@Serializable
data class ExportManifest(
    val format: String,
    val exportFormatVersion: Int,
    val createdAt: String,
    val items: List<ExportManifestEntry>,
) {
    companion object {
        /** 当前导出格式版本。 */
        const val CURRENT_EXPORT_FORMAT_VERSION: Int = 1

        /** 格式标识。 */
        const val FORMAT_NAME: String = "burpremote"
    }
}
