package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

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
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButtonStyle
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTextField
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.localisation.localisedDateTimeFormatter
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 拦截项详情与编辑（plan §43 的 Live/Intercept）。
 *
 * 编辑框可改，但提交要发控制命令，客户端还没有那条通路，因此三个动作按钮都是禁用并写明原因：
 * 点了没反应的按钮比一个禁用的按钮更糟。
 *
 * 标题与返回归装配层的壳；这一屏只负责内容，因此当前状态放在内容第一行，一眼能看到这条还停在队列里。
 */
@Composable
fun InterceptDetailScreen(
    uiState: InterceptDetailUserInterfaceState,
    onIntent: (InterceptDetailUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    InterceptDetailContent(record = record, uiState = uiState, onIntent = onIntent)

                uiState.hasLoaded ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.intercept_detail_not_found_headline),
                        detail = stringResource(R.string.intercept_detail_not_found),
                    )

                else ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.intercept_detail_loading_headline),
                        detail = stringResource(R.string.intercept_detail_loading),
                    )
            }
        }
    }
}

@Composable
private fun InterceptDetailContent(
    record: InterceptRecord,
    uiState: InterceptDetailUserInterfaceState,
    onIntent: (InterceptDetailUserInterfaceIntent) -> Unit,
) {
    val absentValue = stringResource(R.string.intercept_absent_value)

    Section(titleResource = R.string.intercept_detail_summary_heading) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BurpRemoteStatusPill(
                text = stringResource(record.state.labelResource),
                tone = interceptStateTone(state = record.state),
            )
        }
        BurpRemoteTechnicalValue(
            text = "${record.method ?: absentValue} ${record.path ?: absentValue}",
            label = stringResource(R.string.intercept_detail_request_line_label),
        )
        BurpRemoteTechnicalValue(
            text = record.host ?: absentValue,
            label = stringResource(R.string.intercept_detail_host_label),
        )
        BurpRemoteTechnicalValue(
            text = record.sequenceNumber?.toString() ?: absentValue,
            label = stringResource(R.string.intercept_detail_sequence_number_label),
        )
        BurpRemoteTechnicalValue(
            text = INTERCEPT_TIME_FORMATTER.format(record.createdAt),
            label = stringResource(R.string.intercept_detail_created_at_label),
        )
        BurpRemoteTechnicalValue(
            text = INTERCEPT_TIME_FORMATTER.format(record.updatedAt),
            label = stringResource(R.string.intercept_detail_updated_at_label),
        )
    }

    Section(titleResource = R.string.intercept_detail_editor_heading) {
        BurpRemoteEmptyState(
            headline = stringResource(R.string.intercept_detail_editor_headline),
            detail = stringResource(R.string.intercept_detail_editor_local_only),
        )
        BurpRemoteTextField(
            value = uiState.requestLineInput,
            onValueChange = { requestLineInput ->
                onIntent(InterceptDetailUserInterfaceIntent.UpdateRequestLineInput(requestLineInput))
            },
            label = stringResource(R.string.intercept_detail_request_line_label),
            isMonospace = true,
        )
        BurpRemoteTextField(
            value = uiState.requestHeadersInput,
            onValueChange = { requestHeadersInput ->
                onIntent(InterceptDetailUserInterfaceIntent.UpdateRequestHeadersInput(requestHeadersInput))
            },
            label = stringResource(R.string.intercept_detail_request_headers_label),
            isMonospace = true,
        )
    }

    Section(titleResource = R.string.intercept_detail_actions_heading) {
        BurpRemoteButton(
            text = stringResource(R.string.intercept_detail_action_submit),
            onClick = {},
            style = BurpRemoteButtonStyle.Primary,
            isEnabled = uiState.canSendCommand,
        )
        BurpRemoteButton(
            text = stringResource(R.string.intercept_detail_action_forward),
            onClick = {},
            style = BurpRemoteButtonStyle.Secondary,
            isEnabled = uiState.canSendCommand,
        )
        BurpRemoteButton(
            text = stringResource(R.string.intercept_detail_action_drop),
            onClick = {},
            style = BurpRemoteButtonStyle.Danger,
            isEnabled = uiState.canSendCommand,
        )
        BurpRemoteEmptyState(
            headline = stringResource(R.string.intercept_detail_reason_headline),
            detail = stringResource(R.string.intercept_detail_reason_command),
        )
    }
}

/** 状态配色与列表一致：等决定的最需要被看见，已放行是正常流，已丢弃就安静下来。 */
private fun interceptStateTone(state: InterceptState): BurpRemoteStatusTone =
    when (state) {
        InterceptState.Pending, InterceptState.Modified -> BurpRemoteStatusTone.Warning
        InterceptState.Forwarded -> BurpRemoteStatusTone.Live
        InterceptState.Dropped -> BurpRemoteStatusTone.Neutral
    }

/** 一节标题加一块内容；各节的排版一致。 */
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

// 时刻按设备时区与当前语言格式化；队列里两次操作可能只差几秒，因此带秒。
private val INTERCEPT_TIME_FORMATTER: DateTimeFormatter
    get() = localisedDateTimeFormatter(FormatStyle.MEDIUM)

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun InterceptDetailScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        InterceptDetailScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun InterceptDetailScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        InterceptDetailScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "未找到浅色", showBackground = true)
@Composable
private fun InterceptDetailScreenMissingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        InterceptDetailScreen(uiState = InterceptDetailUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "未找到深色", showBackground = true)
@Composable
private fun InterceptDetailScreenMissingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        InterceptDetailScreen(uiState = InterceptDetailUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewState(): InterceptDetailUserInterfaceState =
    InterceptDetailUserInterfaceState(
        record =
            InterceptRecord(
                interceptIdentifier = InterceptIdentifier(value = "intercept_1"),
                sequenceNumber = 5002L,
                createdAt = Instant.parse("2026-09-13T08:00:00Z"),
                updatedAt = Instant.parse("2026-09-13T08:00:01Z"),
                state = InterceptState.Pending,
                host = "api.example.com",
                method = "POST",
                path = "/api/user",
            ),
        hasLoaded = true,
        requestLineInput = "POST /api/user HTTP/1.1",
        requestHeadersInput = "Host: api.example.com\nContent-Type: application/json",
    )
