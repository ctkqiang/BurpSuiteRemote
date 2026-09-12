package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButtonStyle
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 历史详情（plan §20、plan §22）。
 *
 * 只画投影里真有的字段：报文本体要按标识另取，客户端目前没有这条通路，因此那一节写明缺口，
 * 不填占位内容冒充正文（rules.md §5.1）。所有技术值等宽显示，长按可整条复制，
 * 排版上不为了好看截断任何一项——赏金猎人的证据链不能被省略号吃掉。
 *
 * 标题与返回都归装配层的壳；这一屏只负责内容，因此方法、状态码这类一眼判读的标签放在内容第一行。
 */
@Composable
fun HistoryDetailScreen(
    uiState: HistoryDetailUserInterfaceState,
    onIntent: (HistoryDetailUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = BurpRemoteSpacing.Large,
                        vertical = BurpRemoteSpacing.Large,
                    ),
            verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
        ) {
            val record = uiState.record
            when {
                record != null ->
                    RecordDetail(
                        record = record,
                        onShare = {
                            haptics.tap()
                            onIntent(HistoryDetailUserInterfaceIntent.ShareHistoryRecord)
                        },
                    )

                uiState.hasLoaded ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.history_detail_not_found_headline),
                        detail = stringResource(R.string.history_detail_not_found),
                    )

                else ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.history_detail_loading_headline),
                        detail = stringResource(R.string.history_detail_loading),
                    )
            }
        }
    }
}

@Composable
private fun RecordDetail(
    record: HistoryRecord,
    onShare: () -> Unit,
) {
    val absentValue = stringResource(R.string.history_absent_value)

    // 第一行先给判读结果：方法、状态码、归档语义，往下才是逐项明细。
    Section(titleResource = R.string.history_detail_summary_heading) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BurpRemoteStatusPill(
                text = record.method ?: absentValue,
                tone = methodToneOf(record.method),
            )
            BurpRemoteStatusPill(
                text = record.statusCode?.toString() ?: absentValue,
                tone = statusToneOf(record.statusCode),
            )
            BurpRemoteStatusPill(
                text = stringResource(record.archiveState.labelResource),
                tone =
                    if (record.archiveState == HistoryArchiveState.Archived) {
                        BurpRemoteStatusTone.Neutral
                    } else {
                        BurpRemoteStatusTone.Live
                    },
            )
        }
        BurpRemoteTechnicalValue(
            text = "${record.method ?: absentValue} ${record.path ?: absentValue}",
            label = stringResource(R.string.history_detail_request_line_label),
        )
        BurpRemoteTechnicalValue(
            text = record.host ?: absentValue,
            label = stringResource(R.string.history_detail_host_label),
        )
    }

    Section(titleResource = R.string.history_detail_request_heading) {
        BurpRemoteTechnicalValue(
            text = record.scheme ?: absentValue,
            label = stringResource(R.string.history_detail_scheme_label),
        )
        BurpRemoteTechnicalValue(
            text = booleanTextOf(value = record.usesTls, absentValue = absentValue),
            label = stringResource(R.string.history_detail_tls_label),
        )
        BurpRemoteTechnicalValue(
            text = record.destinationInternetProtocolAddress ?: absentValue,
            label = stringResource(R.string.history_detail_destination_address_label),
        )
        BurpRemoteTechnicalValue(
            text = record.listenerPort?.toString() ?: absentValue,
            label = stringResource(R.string.history_detail_listener_port_label),
        )
    }

    Section(titleResource = R.string.history_detail_response_heading) {
        BurpRemoteTechnicalValue(
            text = record.statusCode?.toString() ?: absentValue,
            label = stringResource(R.string.history_detail_status_code_label),
        )
        BurpRemoteTechnicalValue(
            text = record.mimeType ?: absentValue,
            label = stringResource(R.string.history_detail_mime_type_label),
        )
        BurpRemoteTechnicalValue(
            text = responseLengthTextOf(record = record, absentValue = absentValue),
            label = stringResource(R.string.history_detail_response_length_label),
        )
        BurpRemoteTechnicalValue(
            text = durationTextOf(record = record, absentValue = absentValue),
            label = stringResource(R.string.history_detail_duration_label),
        )
    }

    Section(titleResource = R.string.history_detail_record_heading) {
        BurpRemoteStatusPill(
            text = stringResource(record.archiveState.labelResource),
            tone =
                if (record.archiveState == HistoryArchiveState.Archived) {
                    BurpRemoteStatusTone.Neutral
                } else {
                    BurpRemoteStatusTone.Live
                },
        )
        BurpRemoteTechnicalValue(
            text = record.savedAt?.let { savedAt -> RECORD_TIME_FORMATTER.format(savedAt) } ?: absentValue,
            label = stringResource(R.string.history_detail_saved_at_label),
        )
        BurpRemoteTechnicalValue(
            text = record.annotationCount.toString(),
            label = stringResource(R.string.history_detail_annotation_count_label),
        )
        BurpRemoteTechnicalValue(
            text =
                record.lastAnnotatedAt?.let { lastAnnotatedAt -> RECORD_TIME_FORMATTER.format(lastAnnotatedAt) }
                    ?: absentValue,
            label = stringResource(R.string.history_detail_last_annotated_at_label),
        )
        BurpRemoteTechnicalValue(
            text = record.title ?: absentValue,
            label = stringResource(R.string.history_detail_title_label),
        )
        BurpRemoteTechnicalValue(
            text = booleanTextOf(value = record.isEdited, absentValue = absentValue),
            label = stringResource(R.string.history_detail_edited_label),
        )
    }

    Section(titleResource = R.string.history_detail_timeline_heading) {
        BurpRemoteTechnicalValue(
            text = record.sequenceNumber?.toString() ?: absentValue,
            label = stringResource(R.string.history_detail_sequence_number_label),
        )
        BurpRemoteTechnicalValue(
            text = RECORD_TIME_FORMATTER.format(record.occurredAt),
            label = stringResource(R.string.history_detail_occurred_at_label),
        )
    }

    Section(titleResource = R.string.history_detail_message_heading) {
        BurpRemoteEmptyState(
            headline = stringResource(R.string.history_detail_message_headline),
            detail = stringResource(R.string.history_detail_message_unavailable),
        )
        BurpRemoteEmptyState(
            headline = stringResource(R.string.history_detail_reason_headline),
            detail = stringResource(R.string.history_detail_reason_message),
        )
    }

    Section(titleResource = R.string.history_detail_actions_heading) {
        BurpRemoteButton(
            text = stringResource(R.string.history_detail_action_share),
            onClick = onShare,
            style = BurpRemoteButtonStyle.Primary,
        )
        // 送往重放要发控制命令，客户端还没有那条通路；按钮保持禁用并写明原因。
        BurpRemoteButton(
            text = stringResource(R.string.history_detail_action_send_to_repeater),
            onClick = {},
            style = BurpRemoteButtonStyle.Secondary,
            isEnabled = false,
        )
        BurpRemoteEmptyState(
            headline = stringResource(R.string.history_detail_reason_headline),
            detail = stringResource(R.string.history_detail_reason_send_to_repeater),
        )
    }
}

/** 一节标题加一块内容；各节的排版一致，免得同一层级的字段在不同节里长得不一样。 */
@Composable
private fun Section(
    @StringRes titleResource: Int,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(titleResource))
        BurpRemoteCard { content() }
    }
}

@Composable
private fun booleanTextOf(
    value: Boolean?,
    absentValue: String,
): String =
    when (value) {
        true -> stringResource(R.string.history_detail_value_yes)
        false -> stringResource(R.string.history_detail_value_no)
        null -> absentValue
    }

@Composable
private fun responseLengthTextOf(
    record: HistoryRecord,
    absentValue: String,
): String =
    record.responseLength?.let { responseLength ->
        stringResource(R.string.history_response_length_value, responseLength)
    } ?: absentValue

@Composable
private fun durationTextOf(
    record: HistoryRecord,
    absentValue: String,
): String =
    record.durationMilliseconds?.let { durationMilliseconds ->
        stringResource(R.string.history_duration_value, durationMilliseconds)
    } ?: absentValue

/** 方法色标按动词语义分档，与三处列表同一套：读类中性偏在线，写类警告，删除危险。 */
private fun methodToneOf(method: String?): BurpRemoteStatusTone =
    when (method?.uppercase()) {
        METHOD_GET, METHOD_HEAD, METHOD_OPTIONS -> BurpRemoteStatusTone.Live
        METHOD_POST, METHOD_PUT, METHOD_PATCH -> BurpRemoteStatusTone.Warning
        METHOD_DELETE -> BurpRemoteStatusTone.Danger
        else -> BurpRemoteStatusTone.Neutral
    }

/** 状态码分档只看它的百位：2xx 正常、3xx 转向、4xx 客户端错、5xx 服务端错。 */
private fun statusToneOf(statusCode: Int?): BurpRemoteStatusTone =
    when (statusCode?.div(STATUS_CODE_CLASS_DIVISOR)) {
        STATUS_CODE_CLASS_INFORMATIONAL, STATUS_CODE_CLASS_SUCCESS -> BurpRemoteStatusTone.Live
        STATUS_CODE_CLASS_REDIRECTION -> BurpRemoteStatusTone.Neutral
        STATUS_CODE_CLASS_CLIENT_ERROR -> BurpRemoteStatusTone.Warning
        STATUS_CODE_CLASS_SERVER_ERROR -> BurpRemoteStatusTone.Danger
        else -> BurpRemoteStatusTone.Neutral
    }

@get:StringRes
private val HistoryArchiveState.labelResource: Int
    get() =
        when (this) {
            HistoryArchiveState.Live -> R.string.history_detail_archive_state_live
            HistoryArchiveState.Archived -> R.string.history_detail_archive_state_archived
        }

private const val METHOD_GET = "GET"
private const val METHOD_HEAD = "HEAD"
private const val METHOD_OPTIONS = "OPTIONS"
private const val METHOD_POST = "POST"
private const val METHOD_PUT = "PUT"
private const val METHOD_PATCH = "PATCH"
private const val METHOD_DELETE = "DELETE"

private const val STATUS_CODE_CLASS_DIVISOR = 100
private const val STATUS_CODE_CLASS_INFORMATIONAL = 1
private const val STATUS_CODE_CLASS_SUCCESS = 2
private const val STATUS_CODE_CLASS_REDIRECTION = 3
private const val STATUS_CODE_CLASS_CLIENT_ERROR = 4
private const val STATUS_CODE_CLASS_SERVER_ERROR = 5

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
