package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

/**
 * 导出前的敏感数据扫描与脱敏（plan §27）。
 *
 * 纯函数：进出都是文本行，没有 I/O、没有时钟。脱敏只作用于传进来的这份副本——
 * 归档本体在别处，这里既读不到也改不到它（rules.md §12）。
 */
object SensitiveDataRedactor {
    /** 打码用的固定串；长度不跟随原文，免得从长度反推出被遮住的内容。 */
    const val MASK: String = "********"

    /**
     * 逐行扫描。
     *
     * 每一行只报一次：规则按「越靠外越先报」排序（`Authorization: Bearer …` 报 Authorization 而不是
     * Bearer），一行里同一处敏感数据被报两遍只会让用户多划一次屏，不会多知道什么。
     */
    fun scan(lines: List<String>): List<SensitiveDataFinding> =
        lines.mapIndexedNotNull { lineIndex, line ->
            REDACTION_RULES
                .firstOrNull { rule -> rule.pattern.containsMatchIn(line) }
                ?.let { rule -> SensitiveDataFinding(kind = rule.kind, lineNumber = lineIndex + 1) }
        }

    /** 逐行脱敏，返回可以写进导出副本的行。 */
    fun redact(lines: List<String>): List<String> =
        lines.map { line ->
            REDACTION_RULES.fold(line) { redactedLine, rule -> rule.redact(redactedLine) }
        }

    private fun RedactionRule.redact(line: String): String =
        pattern.replace(line) { match ->
            // 分组 0 表示整段命中都要打码（令牌、邮件地址）；否则只遮住敏感值，保留 `Authorization:` 这样的字段名。
            if (secretGroupNumber == 0) {
                MASK
            } else {
                val secretRange = requireNotNull(match.groups[secretGroupNumber]).range
                val prefix = match.value.substring(0, secretRange.first - match.range.first)
                val suffix = match.value.substring(secretRange.last - match.range.first + 1)
                prefix + MASK + suffix
            }
        }

    private class RedactionRule(
        val kind: SensitiveDataKind,
        val pattern: Regex,
        val secretGroupNumber: Int,
    )

    private val REDACTION_RULES: List<RedactionRule> =
        listOf(
            RedactionRule(
                kind = SensitiveDataKind.AuthorizationHeader,
                pattern = Regex("^\\s*authorization\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE),
                secretGroupNumber = 1,
            ),
            RedactionRule(
                kind = SensitiveDataKind.CookieHeader,
                pattern = Regex("^\\s*cookie\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE),
                secretGroupNumber = 1,
            ),
            RedactionRule(
                kind = SensitiveDataKind.SetCookieHeader,
                pattern = Regex("^\\s*set-cookie\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE),
                secretGroupNumber = 1,
            ),
            RedactionRule(
                kind = SensitiveDataKind.ApiKeyHeader,
                pattern = Regex("^\\s*(?:x-api-key|x-apikey|api-key)\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE),
                secretGroupNumber = 1,
            ),
            RedactionRule(
                kind = SensitiveDataKind.BearerToken,
                pattern = Regex("\\bbearer\\s+([A-Za-z0-9\\-._~+/]+=*)", RegexOption.IGNORE_CASE),
                secretGroupNumber = 1,
            ),
            RedactionRule(
                kind = SensitiveDataKind.JsonWebToken,
                pattern = Regex("\\beyJ[A-Za-z0-9_-]{4,}\\.[A-Za-z0-9_-]{4,}\\.[A-Za-z0-9_-]{4,}\\b"),
                secretGroupNumber = 0,
            ),
            RedactionRule(
                kind = SensitiveDataKind.EmailAddress,
                pattern = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
                secretGroupNumber = 0,
            ),
        )
}
