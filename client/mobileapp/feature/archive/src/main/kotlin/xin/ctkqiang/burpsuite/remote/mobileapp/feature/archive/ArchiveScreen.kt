package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.NotImplementedReasonText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant

/**
 * 归档（plan §22、plan §43 的 Archive）。
 *
 * 三个页签各自说明自己的数据从哪来：已保存历史读真实投影，书签与截图两类投影客户端还没有端口，
 * 因此那两个页签写明缺口，不摆假条目。
 */
@Composable
fun ArchiveScreen(
    uiState: ArchiveUserInterfaceState,
    onIntent: (ArchiveUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeading(
                titleResource = R.string.archive_title,
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
            )
            // PrimaryTabRow：TabRow 已经废弃，新代码用这个。
            PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                ArchiveTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == uiState.selectedTab,
                        onClick = { onIntent(ArchiveUserInterfaceIntent.SelectTab(tab)) },
                        text = { Text(text = stringResource(tab.labelResource)) },
                    )
                }
            }
            when (uiState.selectedTab) {
                ArchiveTab.SavedHistory ->
                    SavedHistoryTab(
                        savedRecords = uiState.savedRecords,
                        onIntent = onIntent,
                        modifier = Modifier.weight(1f),
                    )

                ArchiveTab.Bookmarks ->
                    MissingProjectionTab(
                        messageResource = R.string.archive_bookmarks_empty,
                        reasonResource = R.string.archive_reason_bookmarks,
                    )

                ArchiveTab.Screenshots -> ScreenshotsTab(onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun SavedHistoryTab(
    savedRecords: List<HistoryRecord>,
    onIntent: (ArchiveUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (savedRecords.isEmpty()) {
        EmptyStateText(
            messageResource = R.string.archive_saved_history_empty,
            modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
        )
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING, vertical = LIST_VERTICAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        items(items = savedRecords, key = { record -> record.historyIdentifier.value }) { record ->
            SavedRecordRow(record = record, onIntent = onIntent)
        }
    }
}

@Composable
private fun SavedRecordRow(
    record: HistoryRecord,
    onIntent: (ArchiveUserInterfaceIntent) -> Unit,
) {
    val absentValue = stringResource(R.string.archive_absent_value)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onIntent(
                        ArchiveUserInterfaceIntent.OpenSavedRecord(record.historyIdentifier.value),
                    )
                }
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
                text = record.statusCode?.toString() ?: absentValue,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Text(
            text = record.host ?: absentValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.archive_action_share),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier.padding(top = ROW_SPACING).clickable {
                    onIntent(
                        ArchiveUserInterfaceIntent.ShareSavedRecord(record.historyIdentifier.value),
                    )
                },
        )
    }
}

@Composable
private fun MissingProjectionTab(
    @StringRes messageResource: Int,
    @StringRes reasonResource: Int,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        EmptyStateText(messageResource = messageResource)
        NotImplementedReasonText(reasonResource = reasonResource)
    }
}

@Composable
private fun ScreenshotsTab(onIntent: (ArchiveUserInterfaceIntent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        EmptyStateText(messageResource = R.string.archive_screenshots_empty)
        NotImplementedReasonText(reasonResource = R.string.archive_reason_screenshots)
        Button(
            onClick = { onIntent(ArchiveUserInterfaceIntent.OpenScreenshots) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.archive_screenshots_open_action))
        }
    }
}

@get:StringRes
private val ArchiveTab.labelResource: Int
    get() =
        when (this) {
            ArchiveTab.SavedHistory -> R.string.archive_tab_saved_history
            ArchiveTab.Bookmarks -> R.string.archive_tab_bookmarks
            ArchiveTab.Screenshots -> R.string.archive_tab_screenshots
        }

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "已保存浅色", showBackground = true)
@Composable
private fun ArchiveScreenSavedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        ArchiveScreen(uiState = ArchiveUserInterfaceState(savedRecords = previewRecords()), onIntent = {})
    }
}

@Preview(name = "已保存深色", showBackground = true)
@Composable
private fun ArchiveScreenSavedDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        ArchiveScreen(uiState = ArchiveUserInterfaceState(savedRecords = previewRecords()), onIntent = {})
    }
}

@Preview(name = "书签浅色", showBackground = true)
@Composable
private fun ArchiveScreenBookmarksLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        ArchiveScreen(uiState = ArchiveUserInterfaceState(selectedTab = ArchiveTab.Bookmarks), onIntent = {})
    }
}

@Preview(name = "书签深色", showBackground = true)
@Composable
private fun ArchiveScreenBookmarksDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        ArchiveScreen(uiState = ArchiveUserInterfaceState(selectedTab = ArchiveTab.Bookmarks), onIntent = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewRecords(): List<HistoryRecord> =
    listOf(
        previewRecord(identifier = "history_1", method = "GET", path = "/api/login", statusCode = 200),
        previewRecord(identifier = "history_2", method = "POST", path = "/api/user", statusCode = 403),
    )

private fun previewRecord(
    identifier: String,
    method: String,
    path: String,
    statusCode: Int,
): HistoryRecord =
    HistoryRecord(
        historyIdentifier = HistoryIdentifier(value = identifier),
        sequenceNumber = 5004L,
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
        archiveState = HistoryArchiveState.Archived,
        annotationCount = 0,
        lastAnnotatedAt = null,
        savedAt = Instant.parse("2026-09-13T08:01:00Z"),
    )

private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 24.dp
private val LIST_VERTICAL_PADDING = 8.dp
private val ROW_VERTICAL_PADDING = 8.dp
private val ROW_SPACING = 8.dp
