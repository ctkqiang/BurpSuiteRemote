package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** 实时历史（plan §20）。无状态：只显示元数据，每一行都以历史标识作稳定 key（rules.md §8.3）。 */
@Composable
fun HistoryScreen(
    uiState: HistoryUserInterfaceState,
    onOpenHistoryRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            )
            if (uiState.records.isEmpty()) {
                EmptyMessage(modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING))
            } else {
                HistoryRecordList(
                    records = uiState.records,
                    onOpenHistoryRecord = onOpenHistoryRecord,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HistoryRecordList(
    records: List<HistoryRecord>,
    onOpenHistoryRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING, vertical = LIST_VERTICAL_PADDING),
    ) {
        items(items = records, key = { record -> record.historyIdentifier.value }) { record ->
            HistoryRecordRow(
                record = record,
                onOpen = { onOpenHistoryRecord(record.historyIdentifier.value) },
            )
        }
    }
}

@Composable
private fun HistoryRecordRow(
    record: HistoryRecord,
    onOpen: () -> Unit,
) {
    // 这一行只是元数据，不触发任何取正文的动作；正文由详情屏按需取（plan §20）。
    val absentValue = stringResource(R.string.history_absent_value)
    val responseLengthText =
        record.responseLength?.let { responseLength ->
            stringResource(R.string.history_response_length_value, responseLength)
        } ?: absentValue

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(vertical = ROW_VERTICAL_PADDING),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = record.method ?: absentValue,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(ROW_SPACING))
            Text(
                text = record.path ?: absentValue,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(ROW_SPACING))
            Text(text = record.statusCode?.toString() ?: absentValue, style = MaterialTheme.typography.labelLarge)
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = record.host ?: absentValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(ROW_SPACING))
            Text(
                text = responseLengthText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(ROW_SPACING))
            Text(
                text = RECORD_TIME_FORMATTER.format(record.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyMessage(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.history_empty_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

// 时刻按设备时区与当前语言格式化；这里不显示日期，它不在 plan §20 要的元数据里。
private val RECORD_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun HistoryScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryScreen(uiState = HistoryUserInterfaceState(records = previewRecords()), onOpenHistoryRecord = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun HistoryScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryScreen(uiState = HistoryUserInterfaceState(records = previewRecords()), onOpenHistoryRecord = {})
    }
}

@Preview(name = "空列表浅色", showBackground = true)
@Composable
private fun HistoryScreenEmptyLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryScreen(uiState = HistoryUserInterfaceState(), onOpenHistoryRecord = {})
    }
}

@Preview(name = "空列表深色", showBackground = true)
@Composable
private fun HistoryScreenEmptyDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryScreen(uiState = HistoryUserInterfaceState(), onOpenHistoryRecord = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewRecords(): List<HistoryRecord> =
    listOf(
        previewRecord(identifier = "history-1", method = "GET", path = "/api/login", statusCode = 200),
        previewRecord(identifier = "history-2", method = "POST", path = "/api/user", statusCode = 403),
    )

private fun previewRecord(
    identifier: String,
    method: String,
    path: String,
    statusCode: Int,
): HistoryRecord =
    HistoryRecord(
        historyIdentifier = HistoryIdentifier(value = identifier),
        sequenceNumber = 5000L,
        occurredAt = Instant.parse("2026-09-13T08:00:00Z"),
        host = "api.example.com",
        method = method,
        scheme = "https",
        path = path,
        statusCode = statusCode,
        mimeType = "application/json",
        responseLength = 1024L,
        usesTls = true,
        destinationInternetProtocolAddress = "203.0.113.10",
        listenerPort = 8080,
        durationMilliseconds = 42L,
        isEdited = false,
        title = null,
        archiveState = HistoryArchiveState.Live,
        annotationCount = 0,
        lastAnnotatedAt = null,
        savedAt = null,
    )

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val LIST_VERTICAL_PADDING = 8.dp
private val ROW_VERTICAL_PADDING = 8.dp
private val ROW_SPACING = 8.dp
