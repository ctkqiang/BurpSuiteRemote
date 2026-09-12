package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

/**
 * 一次脱敏的结果：扫描了什么、发现了什么、导出副本长什么样。
 *
 * @property scannedLines 实际被扫描的行。
 * @property findings 命中的敏感数据位置。
 * @property redactedLines 脱敏之后、准备写进导出副本的行。
 */
data class RedactionSummary(
    val scannedLines: List<String>,
    val findings: List<SensitiveDataFinding>,
    val redactedLines: List<String>,
)
