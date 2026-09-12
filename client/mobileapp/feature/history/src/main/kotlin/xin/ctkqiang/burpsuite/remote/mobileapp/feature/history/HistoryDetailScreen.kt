package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LabelValueText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.NotImplementedReasonText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.SectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 历史详情（plan §20、plan §22）。
 *
 * 只画投影里真有的字段：报文本体要按标识另取，客户端目前没有这条通路，因此那一节写明缺口，
 * 不填占位内容冒充正文（rules.md §5.1）。
 */
@Composable
fun HistoryDetailScreen(
    uiState: HistoryDetailUserInterfaceState,
    onIntent: (HistoryDetailUserInterfaceIntent) -> Unit,
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
            ScreenHeading(titleResource = R.string.history_detail_title)
            val record = uiState.record
            when {
                record != null -> RecordDetail(record = record, onIntent = onIntent)
                uiState.hasLoaded -> EmptyStateText(messageResource = R.string.history_detail_not_found)
                else -> EmptyStateText(messageResource = R.string.history_detail_loading)
            }
        }
    }
}

@Composable
private fun RecordDetail(
    record: HistoryRecord,
    onIntent: (HistoryDetailUserInterfaceIntent) -> Unit,
) {
    val absentValue = stringResource(R.string.history_absent_value)
    val yes = stringResource(R.string.history_detail_value_yes)
    val no = stringResource(R.string.history_detail_value_no)

    RequestSection(record = record, absentValue = absentValue, yes = yes, no = no)
    ResponseSection(record = record, absentValue = absentValue)
    RecordSection(record = record, absentValue = absentValue, yes = yes, no = no)
    TimelineSection(record = record, absentValue = absentValue)
    MessageSection()
    Actions(onIntent = onIntent)
}

@Composable
private fun RequestSection(
    record: HistoryRecord,
    absentValue: String,
    yes: String,
    no: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.history_detail_request_heading)
        LabelValueText(
            labelResource = R.string.history_detail_request_line_label,
            value = "${record.method ?: absentValue} ${record.path ?: absentValue}",
        )
        LabelValueText(labelResource = R.string.history_detail_host_label, value = record.host ?: absentValue)
        LabelValueText(labelResource = R.string.history_detail_scheme_label, value = record.scheme ?: absentValue)
        LabelValueText(
            labelResource = R.string.history_detail_tls_label,
            value = record.usesTls?.let { usesTls -> if (usesTls) yes else no } ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.history_detail_destination_address_label,
            value = record.destinationInternetProtocolAddress ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.history_detail_listener_port_label,
            value = record.listenerPort?.toString() ?: absentValue,
        )
    }
}

@Composable
private fun ResponseSection(
    record: HistoryRecord,
    absentValue: String,
) {
    val responseLengthText =
        record.responseLength?.let { responseLength ->
            stringResource(R.string.history_response_length_value, responseLength)
        } ?: absentValue
    val durationText =
        record.durationMilliseconds?.let { durationMilliseconds ->
            stringResource(R.string.history_detail_duration_value, durationMilliseconds)
        } ?: absentValue

    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.history_detail_response_heading)
        LabelValueText(
            labelResource = R.string.history_detail_status_code_label,
            value = record.statusCode?.toString() ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.history_detail_mime_type_label,
            value = record.mimeType ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.history_detail_response_length_label,
            value = responseLengthText,
        )
        LabelValueText(labelResource = R.string.history_detail_duration_label, value = durationText)
    }
}

@Composable
private fun RecordSection(
    record: HistoryRecord,
    absentValue: String,
    yes: String,
    no: String,
) {
    val archiveStateText =
        stringResource(
            when (record.archiveState) {
                HistoryArchiveState.Live -> R.string.history_detail_archive_state_live
                HistoryArchiveState.Archived -> R.string.history_detail_archive_state_archived
            },
        )

    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.history_detail_record_heading)
        LabelValueText(labelResource = R.string.history_detail_archive_state_label, value = archiveStateText)
        LabelValueText(
            labelResource = R.string.history_detail_saved_at_label,
            value = record.savedAt?.let { savedAt -> RECORD_TIME_FORMATTER.format(savedAt) } ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.history_detail_annotation_count_label,
            value = record.annotationCount.toString(),
        )
        LabelValueText(
            labelResource = R.string.history_detail_last_annotated_at_label,
            value =
                record.lastAnnotatedAt?.let { lastAnnotatedAt ->
                    RECORD_TIME_FORMATTER.format(lastAnnotatedAt)
                } ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.history_detail_title_label,
            value = record.title ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.history_detail_edited_label,
            value = if (record.isEdited) yes else no,
        )
    }
}

@Composable
private fun TimelineSection(
    record: HistoryRecord,
    absentValue: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.history_detail_timeline_heading)
        LabelValueText(
            labelResource = R.string.history_detail_sequence_number_label,
            value = record.sequenceNumber?.toString() ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.history_detail_occurred_at_label,
            value = RECORD_TIME_FORMATTER.format(record.occurredAt),
        )
    }
}

@Composable
private fun MessageSection() {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.history_detail_message_heading)
        EmptyStateText(messageResource = R.string.history_detail_message_unavailable)
        NotImplementedReasonText(reasonResource = R.string.history_detail_reason_message)
    }
}

@Composable
private fun Actions(onIntent: (HistoryDetailUserInterfaceIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        Button(
            onClick = { onIntent(HistoryDetailUserInterfaceIntent.ShareHistoryRecord) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.history_detail_action_share))
        }
        // 送往重放要发控制命令，客户端还没有那条通路；按钮保持禁用并写明原因。
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.history_detail_action_send_to_repeater))
        }
        NotImplementedReasonText(reasonResource = R.string.history_detail_reason_send_to_repeater)
    }
}

// 时刻按设备时区与当前语言格式化；归档语义靠秒级时间区分，所以这里带日期。
private val RECORD_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun HistoryDetailScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryDetailScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun HistoryDetailScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryDetailScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "未找到浅色", showBackground = true)
@Composable
private fun HistoryDetailScreenMissingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryDetailScreen(uiState = HistoryDetailUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "未找到深色", showBackground = true)
@Composable
private fun HistoryDetailScreenMissingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryDetailScreen(uiState = HistoryDetailUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewState(): HistoryDetailUserInterfaceState =
    HistoryDetailUserInterfaceState(
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
                title = null,
                archiveState = HistoryArchiveState.Archived,
                annotationCount = 2,
                lastAnnotatedAt = Instant.parse("2026-09-13T08:05:00Z"),
                savedAt = Instant.parse("2026-09-13T08:01:00Z"),
            ),
        hasLoaded = true,
    )

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
