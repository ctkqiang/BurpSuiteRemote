package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState
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
 * 拦截项详情与编辑（plan §43 的 Live/Intercept）。
 *
 * 编辑框可改，但提交要发控制命令，客户端还没有那条通路，因此三个动作按钮都是禁用并写明原因。
 */
@Composable
fun InterceptDetailScreen(
    uiState: InterceptDetailUserInterfaceState,
    onIntent: (InterceptDetailUserInterfaceIntent) -> Unit,
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
            ScreenHeading(titleResource = R.string.intercept_detail_title)
            val record = uiState.record
            when {
                record != null -> InterceptDetailContent(record = record, uiState = uiState, onIntent = onIntent)
                uiState.hasLoaded -> EmptyStateText(messageResource = R.string.intercept_detail_not_found)
                else -> EmptyStateText(messageResource = R.string.intercept_detail_loading)
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

    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.intercept_detail_request_heading)
        LabelValueText(
            labelResource = R.string.intercept_detail_request_line_label,
            value = "${record.method ?: absentValue} ${record.path ?: absentValue}",
        )
        LabelValueText(labelResource = R.string.intercept_detail_host_label, value = record.host ?: absentValue)
        LabelValueText(
            labelResource = R.string.intercept_detail_state_label,
            value = stringResource(record.state.labelResource),
        )
        LabelValueText(
            labelResource = R.string.intercept_detail_sequence_number_label,
            value = record.sequenceNumber?.toString() ?: absentValue,
        )
        LabelValueText(
            labelResource = R.string.intercept_detail_created_at_label,
            value = INTERCEPT_TIME_FORMATTER.format(record.createdAt),
        )
        LabelValueText(
            labelResource = R.string.intercept_detail_updated_at_label,
            value = INTERCEPT_TIME_FORMATTER.format(record.updatedAt),
        )
    }

    InterceptEditor(uiState = uiState, onIntent = onIntent)
    InterceptActions(uiState = uiState)
}

@Composable
private fun InterceptEditor(
    uiState: InterceptDetailUserInterfaceState,
    onIntent: (InterceptDetailUserInterfaceIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.intercept_detail_editor_heading)
        Text(
            text = stringResource(R.string.intercept_detail_editor_local_only),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.requestLineInput,
            onValueChange = { requestLineInput ->
                onIntent(InterceptDetailUserInterfaceIntent.UpdateRequestLineInput(requestLineInput))
            },
            label = { Text(text = stringResource(R.string.intercept_detail_request_line_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.requestHeadersInput,
            onValueChange = { requestHeadersInput ->
                onIntent(InterceptDetailUserInterfaceIntent.UpdateRequestHeadersInput(requestHeadersInput))
            },
            label = { Text(text = stringResource(R.string.intercept_detail_request_headers_label)) },
            modifier = Modifier.fillMaxWidth().height(HEADERS_FIELD_HEIGHT),
        )
    }
}

@Composable
private fun InterceptActions(uiState: InterceptDetailUserInterfaceState) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.intercept_detail_actions_heading)
        Button(onClick = {}, enabled = uiState.canSendCommand, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.intercept_detail_action_submit))
        }
        Button(onClick = {}, enabled = uiState.canSendCommand, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.intercept_detail_action_forward))
        }
        Button(onClick = {}, enabled = uiState.canSendCommand, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.intercept_detail_action_drop))
        }
        NotImplementedReasonText(reasonResource = R.string.intercept_detail_reason_command)
    }
}

// 时刻按设备时区与当前语言格式化；队列里两次操作可能只差几秒，因此带秒。
private val INTERCEPT_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

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
        InterceptDetailScreen(
            uiState = InterceptDetailUserInterfaceState(hasLoaded = true),
            onIntent = {},
        )
    }
}

@Preview(name = "未找到深色", showBackground = true)
@Composable
private fun InterceptDetailScreenMissingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        InterceptDetailScreen(
            uiState = InterceptDetailUserInterfaceState(hasLoaded = true),
            onIntent = {},
        )
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

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
private val HEADERS_FIELD_HEIGHT = 160.dp
