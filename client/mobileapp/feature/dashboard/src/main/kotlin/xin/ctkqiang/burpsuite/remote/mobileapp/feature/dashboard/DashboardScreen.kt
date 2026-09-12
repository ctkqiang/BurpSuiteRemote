package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ConnectionStateIndicator
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LabelValueText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LiveOfflineBadge
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.SectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant

/** 主面板（plan §44）。无状态：状态由外面传进来，用户意图往外抛。 */
@Composable
fun DashboardScreen(
    uiState: DashboardUserInterfaceState,
    onIntent: (DashboardUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            item(key = HEADER_KEY) { Header(uiState = uiState) }
            item(key = COUNTS_KEY) { CountsRow(uiState = uiState) }
            item(key = ENTRIES_KEY) { EntrySection(onIntent = onIntent) }
            item(key = RECENT_HEADING_KEY) {
                SectionHeading(titleResource = R.string.dashboard_recent_heading)
            }
            if (uiState.recentRecords.isEmpty()) {
                item(key = RECENT_EMPTY_KEY) {
                    EmptyStateText(messageResource = R.string.dashboard_recent_empty)
                }
            } else {
                items(
                    items = uiState.recentRecords,
                    key = { record -> record.historyIdentifier.value },
                ) { record ->
                    RecentRecordRow(
                        record = record,
                        onOpen = {
                            onIntent(
                                DashboardUserInterfaceIntent.OpenHistoryRecord(record.historyIdentifier.value),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(uiState: DashboardUserInterfaceState) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        Text(text = stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            // LIVE / OFFLINE 与后面那行本地化状态名是两个层次：前者一眼看有无，后者说明卡在哪一步。
            LiveOfflineBadge(connectionState = uiState.connectionState)
            Spacer(modifier = Modifier.width(ROW_SPACING))
            ConnectionStateIndicator(connectionState = uiState.connectionState)
        }
        LabelValueText(
            labelResource = R.string.dashboard_target_host_label,
            // 没有记录时显示「不知道」，而不是编一个地址。
            value = uiState.targetHost ?: stringResource(R.string.dashboard_absent_value),
        )
    }
}

@Composable
private fun CountsRow(uiState: DashboardUserInterfaceState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        CountCard(
            value = uiState.liveRequestCount,
            labelResource = R.string.dashboard_live_request_label,
            modifier = Modifier.weight(1f),
        )
        CountCard(
            value = uiState.interceptedCount,
            labelResource = R.string.dashboard_intercepted_label,
            modifier = Modifier.weight(1f),
        )
        CountCard(
            value = uiState.savedCount,
            labelResource = R.string.dashboard_saved_label,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CountCard(
    value: Int,
    @StringRes labelResource: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(CARD_PADDING)) {
            Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(labelResource),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntrySection(onIntent: (DashboardUserInterfaceIntent) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        Button(
            onClick = { onIntent(DashboardUserInterfaceIntent.OpenLiveHistory) },
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.dashboard_entry_history))
        }
        Button(
            onClick = { onIntent(DashboardUserInterfaceIntent.OpenLiveIntercept) },
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.dashboard_entry_intercept))
        }
        Button(
            onClick = { onIntent(DashboardUserInterfaceIntent.OpenLiveRepeater) },
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.dashboard_entry_repeater))
        }
    }
}

@Composable
private fun RecentRecordRow(
    record: HistoryRecord,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = record.method ?: stringResource(R.string.dashboard_absent_value),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(ROW_SPACING))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.host ?: stringResource(R.string.dashboard_absent_value),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = record.path ?: stringResource(R.string.dashboard_absent_value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(ROW_SPACING))
        Text(
            text = record.statusCode?.toString() ?: stringResource(R.string.dashboard_absent_value),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun DashboardScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        DashboardScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun DashboardScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        DashboardScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "离线浅色", showBackground = true)
@Composable
private fun DashboardScreenOfflineLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        DashboardScreen(uiState = DashboardUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "离线深色", showBackground = true)
@Composable
private fun DashboardScreenOfflineDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        DashboardScreen(uiState = DashboardUserInterfaceState(), onIntent = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun populatedPreviewState(): DashboardUserInterfaceState =
    DashboardUserInterfaceState(
        connectionState = ConnectionState.Connected,
        targetHost = "api.example.com",
        liveRequestCount = 1284,
        interceptedCount = 23,
        savedCount = 87,
        recentRecords =
            listOf(
                previewRecord(method = "GET", path = "/api/login", statusCode = 200),
                previewRecord(method = "POST", path = "/api/user", statusCode = 403),
            ),
    )

private fun previewRecord(
    method: String,
    path: String,
    statusCode: Int,
): HistoryRecord =
    HistoryRecord(
        historyIdentifier = HistoryIdentifier(value = "$method $path"),
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

private const val HEADER_KEY = "header"
private const val COUNTS_KEY = "counts"
private const val ENTRIES_KEY = "entries"
private const val RECENT_HEADING_KEY = "recent-heading"
private const val RECENT_EMPTY_KEY = "recent-empty"

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val SECTION_SPACING = 24.dp
private val ROW_SPACING = 8.dp
private val CARD_PADDING = 12.dp
