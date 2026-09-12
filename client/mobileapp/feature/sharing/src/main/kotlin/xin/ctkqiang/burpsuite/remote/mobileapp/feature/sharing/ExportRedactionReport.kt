package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import kotlinx.serialization.Serializable

/**
 * 导出包里的脱敏报告（plan §27）。导入方据此知道哪些内容被改写过，以及扫描范围有多大。
 *
 * @property exportedAt 导出时刻，ISO-8601 文本。
 * @property scannedLineCount 扫描过的行数。
 * @property findings 命中的敏感数据位置；不含原文。
 */
@Serializable
data class ExportRedactionReport(
    val exportedAt: String,
    val scannedLineCount: Int,
    val findings: List<ExportRedactionFinding>,
) {
    /** 报告里的一条命中；只记种类与行号，不记原文。 */
    @Serializable
    data class ExportRedactionFinding(
        val kind: String,
        val lineNumber: Int,
    )
}
