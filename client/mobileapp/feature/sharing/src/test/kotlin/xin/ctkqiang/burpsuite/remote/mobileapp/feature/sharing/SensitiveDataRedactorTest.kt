package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 脱敏是安全相关的纯逻辑，先把行为钉住再谈界面。 */
class SensitiveDataRedactorTest {
    @Test
    fun `scan reports the kind and line number of an authorization header`() {
        val findings = SensitiveDataRedactor.scan(listOf("method=GET", "Authorization: Bearer abc123"))

        assertEquals(listOf(SensitiveDataFinding(SensitiveDataKind.AuthorizationHeader, 2)), findings)
    }

    @Test
    fun `scan reports a bearer token inside a free-form value`() {
        val findings = SensitiveDataRedactor.scan(listOf("header=x-auth Bearer abc.def.ghi"))

        assertTrue(findings.any { finding -> finding.kind == SensitiveDataKind.BearerToken })
    }

    @Test
    fun `scan reports a json web token by shape`() {
        val findings = SensitiveDataRedactor.scan(listOf("token=$JSON_WEB_TOKEN"))

        assertEquals(listOf(SensitiveDataFinding(SensitiveDataKind.JsonWebToken, 1)), findings)
    }

    @Test
    fun `scan reports an e-mail address`() {
        val findings = SensitiveDataRedactor.scan(listOf("title=session for analyst@example.com"))

        assertEquals(listOf(SensitiveDataFinding(SensitiveDataKind.EmailAddress, 1)), findings)
    }

    @Test
    fun `scan reports nothing for plain metadata`() {
        val findings = SensitiveDataRedactor.scan(listOf("method=GET", "host=api.example.com", "path=/api/user"))

        assertEquals(emptyList<SensitiveDataFinding>(), findings)
    }

    @Test
    fun `redact keeps the field name and masks the value`() {
        val redactedLines = SensitiveDataRedactor.redact(listOf("Authorization: Bearer abc123"))

        assertEquals(listOf("Authorization: " + SensitiveDataRedactor.MASK), redactedLines)
    }

    @Test
    fun `redact masks a standalone token without touching the surrounding text`() {
        val line = "note=token is $JSON_WEB_TOKEN today"

        val redactedLines = SensitiveDataRedactor.redact(listOf(line))

        assertEquals(listOf("note=token is " + SensitiveDataRedactor.MASK + " today"), redactedLines)
    }

    @Test
    fun `redact leaves a line without sensitive data untouched`() {
        val originalLines = listOf("method=GET", "host=api.example.com")

        assertEquals(originalLines, SensitiveDataRedactor.redact(originalLines))
    }

    @Test
    fun `redact never leaks the masked value length`() {
        val shortSecret = SensitiveDataRedactor.redact(listOf("x-api-key: a"))
        val longSecret = SensitiveDataRedactor.redact(listOf("x-api-key: a-very-long-secret-value"))

        assertEquals(shortSecret, longSecret)
    }

    private companion object {
        // 结构像 JWT 即可：三段 base64url，第一段以 eyJ 开头。
        const val JSON_WEB_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abcdefghij"
    }
}
