package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant

/** 拦截队列（plan §43 的 Live/Intercept）。无状态：只显示投影，每一行以拦截标识作稳定 key。 */
@Composable
fun InterceptScreen(
    uiState: InterceptUserInterfaceState,
    onIntent: (InterceptUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeading(
                titleResource = R.string.intercept_title,
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            )
            if (uiState.records.isEmpty()) {
                EmptyStateText(
                    messageResource = R.string.intercept_empty,
                    modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING),
                )
            } else {
                InterceptRecordList(
                    records = uiState.records,
                    onIntent = onIntent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InterceptRecordList(
    records: List<InterceptRecord>,
    onIntent: (InterceptUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING, vertical = LIST_VERTICAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        items(items = records, key = { record -> record.interceptIdentifier.value }) { record ->
            InterceptRecordRow(
                record = record,
                onOpen = {
                    onIntent(
                        InterceptUserInterfaceIntent.OpenInterceptRecord(record.interceptIdentifier.value),
                    )
                },
            )
        }
    }
}

@Composable
private fun InterceptRecordRow(
    record: InterceptRecord,
    onOpen: () -> Unit,
) {
    val absentValue = stringResource(R.string.intercept_absent_value)
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
            Text(
                text = stringResource(record.state.labelResource),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = record.host ?: absentValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@get:StringRes
internal val InterceptState.labelResource: Int
    get() =
        when (this) {
            InterceptState.Pending -> R.string.intercept_state_pending
            InterceptState.Modified -> R.string.intercept_state_modified
            InterceptState.Forwarded -> R.string.intercept_state_forwarded
            InterceptState.Dropped -> R.string.intercept_state_dropped
        }

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun InterceptScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        InterceptScreen(uiState = InterceptUserInterfaceState(records = previewRecords()), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun InterceptScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        InterceptScreen(uiState = InterceptUserInterfaceState(records = previewRecords()), onIntent = {})
    }
}

@Preview(name = "空队列浅色", showBackground = true)
@Composable
private fun InterceptScreenEmptyLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        InterceptScreen(uiState = InterceptUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "空队列深色", showBackground = true)
@Composable
private fun InterceptScreenEmptyDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        InterceptScreen(uiState = InterceptUserInterfaceState(), onIntent = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewRecords(): List<InterceptRecord> =
    listOf(
        previewRecord(identifier = "intercept_1", state = InterceptState.Pending, method = "POST", path = "/api/user"),
        previewRecord(identifier = "intercept_2", state = InterceptState.Modified, method = "GET", path = "/api/admin"),
    )

private fun previewRecord(
    identifier: String,
    state: InterceptState,
    method: String,
    path: String,
): InterceptRecord =
    InterceptRecord(
        interceptIdentifier = InterceptIdentifier(value = identifier),
        sequenceNumber = 5002L,
        createdAt = Instant.parse("2026-09-13T08:00:00Z"),
        updatedAt = Instant.parse("2026-09-13T08:00:01Z"),
        state = state,
        host = "api.example.com",
        method = method,
        path = path,
    )

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val LIST_VERTICAL_PADDING = 8.dp
private val ROW_VERTICAL_PADDING = 8.dp
private val ROW_SPACING = 8.dp
