package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

/**
 * 一处敏感数据。
 *
 * 只记位置与种类，**不记命中的原文**：这份结论要显示出来，也要写进导出包的报告里，
 * 把原文带上等于把要防的东西又抄了一遍（rules.md §12）。
 *
 * @property kind 命中的敏感数据种类。
 * @property lineNumber 命中的行号，从 1 开始。
 */
data class SensitiveDataFinding(
    val kind: SensitiveDataKind,
    val lineNumber: Int,
)
