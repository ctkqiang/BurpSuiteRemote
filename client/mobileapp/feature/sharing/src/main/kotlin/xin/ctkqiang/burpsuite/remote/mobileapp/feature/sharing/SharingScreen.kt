package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LabelValueText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.SectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant

/**
 * 分享与导出（plan §75）。
 *
 * 顺序是固定的：看清导出范围 → 看敏感数据扫描结论 → 看脱敏后的副本 → 用户确认 → 才写出去。
 * 脱敏只作用于导出副本，原始归档在这里既读不到也改不到。
 */
@Composable
fun SharingScreen(
    uiState: SharingUserInterfaceState,
    onIntent: (SharingUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            ScreenHeading(titleResource = R.string.sharing_title)
            val record = uiState.record
            when {
                record != null ->
                    ExportGate(
                        record = record,
                        redaction = uiState.redaction,
                        hasExported = uiState.hasExported,
                        hasExportFailed = uiState.hasExportFailed,
                        onIntent = onIntent,
                    )

                uiState.hasLoaded -> EmptyStateText(messageResource = R.string.sharing_not_found)
                else -> EmptyStateText(messageResource = R.string.sharing_loading)
            }
        }
    }
}

@Composable
private fun ExportGate(
    record: HistoryRecord,
    redaction: RedactionSummary?,
    hasExported: Boolean,
    hasExportFailed: Boolean,
    onIntent: (SharingUserInterfaceIntent) -> Unit,
) {
    val absentValue = stringResource(R.string.sharing_absent_value)
    LabelValueText(
        labelResource = R.string.sharing_request_line_label,
        value = "${record.method ?: absentValue} ${record.path ?: absentValue}",
    )
    LabelValueText(labelResource = R.string.sharing_host_label, value = record.host ?: absentValue)
    ScopeSection()
    if (redaction != null) {
        ScanSection(redaction = redaction)
        PreviewSection(redaction = redaction)
    }
    Button(onClick = { onIntent(SharingUserInterfaceIntent.ConfirmExport) }, modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.sharing_confirm_action))
    }
    if (hasExported) {
        EmptyStateText(messageResource = R.string.sharing_exported)
    }
    if (hasExportFailed) {
        EmptyStateText(messageResource = R.string.sharing_export_failed)
    }
}

@Composable
private fun ScopeSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.sharing_scope_heading)
        EmptyStateText(messageResource = R.string.sharing_scope_note)
    }
}

@Composable
private fun ScanSection(redaction: RedactionSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.sharing_scan_heading)
        Text(
            text = stringResource(R.string.sharing_scan_summary, redaction.scannedLines.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (redaction.findings.isEmpty()) {
            EmptyStateText(messageResource = R.string.sharing_scan_empty)
            return@Column
        }
        redaction.findings.forEach { finding ->
            Text(
                text =
                    stringResource(
                        R.string.sharing_scan_finding,
                        finding.lineNumber,
                        stringResource(finding.kind.labelResource),
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PreviewSection(redaction: RedactionSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.sharing_preview_heading)
        EmptyStateText(messageResource = R.string.sharing_preview_note)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CARD_PADDING),
                verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
            ) {
                redaction.redactedLines.forEach { redactedLine ->
                    Text(
                        text = redactedLine,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@get:StringRes
private val SensitiveDataKind.labelResource: Int
    get() =
        when (this) {
            SensitiveDataKind.AuthorizationHeader -> R.string.sharing_kind_authorization_header
            SensitiveDataKind.CookieHeader -> R.string.sharing_kind_cookie_header
            SensitiveDataKind.SetCookieHeader -> R.string.sharing_kind_set_cookie_header
            SensitiveDataKind.ApiKeyHeader -> R.string.sharing_kind_api_key_header
            SensitiveDataKind.BearerToken -> R.string.sharing_kind_bearer_token
            SensitiveDataKind.JsonWebToken -> R.string.sharing_kind_json_web_token
            SensitiveDataKind.EmailAddress -> R.string.sharing_kind_email_address
        }

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun SharingScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        SharingScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun SharingScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        SharingScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "已导出浅色", showBackground = true)
@Composable
private fun SharingScreenExportedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        SharingScreen(uiState = previewState(hasExported = true), onIntent = {})
    }
}

@Preview(name = "已导出深色", showBackground = true)
@Composable
private fun SharingScreenExportedDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        SharingScreen(uiState = previewState(hasExported = true), onIntent = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewState(hasExported: Boolean = false): SharingUserInterfaceState {
    val scannedLines =
        listOf(
            "method=GET",
            "host=api.example.com",
            "path=/api/user",
            "title=session for analyst@example.com",
        )
    return SharingUserInterfaceState(
        record =
            HistoryRecord(
                historyIdentifier = HistoryIdentifier(value = "history_123"),
                sequenceNumber = 5001L,
                occurredAt = Instant.parse("2026-09-13T08:00:00Z"),
                host = "api.example.com",
                method = "GET",
                scheme = "https",
                path = "/api/user",
                statusCode = 200,
                mimeType = "application/json",
                responseLength = 1024L,
                usesTls = true,
                destinationInternetProtocolAddress = "203.0.113.10",
                listenerPort = 8080,
                durationMilliseconds = 42L,
                isEdited = false,
                title = "session for analyst@example.com",
                archiveState = HistoryArchiveState.Archived,
                annotationCount = 0,
                lastAnnotatedAt = null,
                savedAt = Instant.parse("2026-09-13T08:01:00Z"),
            ),
        hasLoaded = true,
        redaction =
            RedactionSummary(
                scannedLines = scannedLines,
                findings = SensitiveDataRedactor.scan(scannedLines),
                redactedLines = SensitiveDataRedactor.redact(scannedLines),
            ),
        hasExported = hasExported,
    )
}

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
private val CARD_PADDING = 12.dp
