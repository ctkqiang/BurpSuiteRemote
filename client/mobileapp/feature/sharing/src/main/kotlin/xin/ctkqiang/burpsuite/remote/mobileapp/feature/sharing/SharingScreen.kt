package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButtonStyle
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant

/**
 * 分享与导出（plan §75）。
 *
 * 顺序是固定的：看清导出范围 → 看敏感数据扫描结论 → 看脱敏后的副本 → 用户确认 → 才写出去。
 * 脱敏只作用于导出副本，原始归档在这里既读不到也改不到；脱敏后的预览是等宽文本，长按可整段复制。
 */
@Composable
fun SharingScreen(
    uiState: SharingUserInterfaceState,
    onIntent: (SharingUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()

    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        run {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SCREEN_PADDING, vertical = SCREEN_PADDING),
                verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
            ) {
                val record = uiState.record
                when {
                    record != null ->
                        ExportGate(
                            record = record,
                            uiState = uiState,
                            onExport = {
                                haptics.tap()
                                onIntent(SharingUserInterfaceIntent.ConfirmExport)
                            },
                        )

                    uiState.hasLoaded ->
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.sharing_not_found_headline),
                            detail = stringResource(R.string.sharing_not_found),
                        )

                    else ->
                        BurpRemoteEmptyState(
                            headline = stringResource(R.string.sharing_loading_headline),
                            detail = stringResource(R.string.sharing_loading),
                        )
                }
            }
        }
    }
}

@Composable
private fun ExportGate(
    record: HistoryRecord,
    uiState: SharingUserInterfaceState,
    onExport: () -> Unit,
) {
    val absentValue = stringResource(R.string.sharing_absent_value)

    Section(titleResource = R.string.sharing_subject_heading) {
        BurpRemoteTechnicalValue(
            text = "${record.method ?: absentValue} ${record.path ?: absentValue}",
            label = stringResource(R.string.sharing_request_line_label),
        )
        BurpRemoteTechnicalValue(
            text = record.host ?: absentValue,
            label = stringResource(R.string.sharing_host_label),
        )
        BurpRemoteTechnicalValue(
            text = record.historyIdentifier.value,
            label = stringResource(R.string.sharing_identifier_label),
        )
    }

    Section(titleResource = R.string.sharing_scope_heading) {
        BurpRemoteEmptyState(
            headline = stringResource(R.string.sharing_scope_headline),
            detail = stringResource(R.string.sharing_scope_note),
        )
    }

    val redaction = uiState.redaction
    if (redaction != null) {
        Section(titleResource = R.string.sharing_scan_heading) {
            BurpRemoteTechnicalValue(
                text = redaction.scannedLines.size.toString(),
                label = stringResource(R.string.sharing_scan_count_label),
            )
            if (redaction.findings.isEmpty()) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.sharing_scan_empty_headline),
                    detail = stringResource(R.string.sharing_scan_empty),
                )
            } else {
                redaction.findings.forEach { finding ->
                    BurpRemoteTechnicalValue(
                        text = finding.lineNumber.toString(),
                        label = stringResource(finding.kind.labelResource),
                    )
                }
            }
        }

        Section(titleResource = R.string.sharing_preview_heading) {
            BurpRemoteEmptyState(
                headline = stringResource(R.string.sharing_preview_headline),
                detail = stringResource(R.string.sharing_preview_note),
            )
            BurpRemoteCard {
                redaction.redactedLines.forEach { redactedLine ->
                    BurpRemoteTechnicalValue(text = redactedLine)
                }
            }
        }
    }

    Section(titleResource = R.string.sharing_confirm_heading) {
        BurpRemoteButton(
            text = stringResource(R.string.sharing_confirm_action),
            onClick = onExport,
            style = BurpRemoteButtonStyle.Primary,
        )
        if (uiState.hasExported) {
            BurpRemoteEmptyState(
                headline = stringResource(R.string.sharing_state_exported),
                detail = stringResource(R.string.sharing_exported),
            )
        }
        if (uiState.hasExportFailed) {
            BurpRemoteEmptyState(
                headline = stringResource(R.string.sharing_state_failed),
                detail = stringResource(R.string.sharing_export_failed),
            )
        }
    }
}

/** 一节标题加一块内容；各节的排版一致。 */
@Composable
private fun Section(
    @StringRes titleResource: Int,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        BurpRemoteSectionHeading(text = stringResource(titleResource))
        content()
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

private val SCREEN_PADDING = 16.dp
private val SECTION_SPACING = 20.dp
private val ROW_SPACING = 8.dp

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
