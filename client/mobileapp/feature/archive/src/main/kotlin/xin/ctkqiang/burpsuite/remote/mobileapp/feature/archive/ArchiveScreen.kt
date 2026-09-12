package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemotePullToRefresh
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.burpRemoteStaggeredEntry
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 归档（plan §22、plan §43 的 Archive）。
 *
 * 三个页签各自说明自己的数据从哪来：已保存历史读真实投影，书签与截图两类投影客户端还没有端口，
 * 因此那两个页签写明缺口，不摆假条目。页签切换给一记选中触感，切换后新内容按错峰入场落下，
 * 于是「换了一组数据」这件事在视觉上有交代。
 *
 * 整个页面只有一个滚动容器（[LazyColumn]），页签行是它的第一项——把页签放在 LazyColumn 外面
 * 再嵌一层滚动容器，会让内层列表拿不到有限高度。
 */
@Composable
fun ArchiveScreen(
    uiState: ArchiveUserInterfaceState,
    onIntent: (ArchiveUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    val haptics = rememberBurpRemoteHaptics()

    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        BurpRemotePullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onIntent(ArchiveUserInterfaceIntent.Refresh) },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        horizontal = BurpRemoteSpacing.Large,
                        vertical = BurpRemoteSpacing.Large,
                    ),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            ) {
                item(key = TABS_KEY) {
                    ArchiveTabRow(
                        selectedTab = uiState.selectedTab,
                        onSelectTab = { tab ->
                            haptics.select()
                            onIntent(ArchiveUserInterfaceIntent.SelectTab(tab))
                        },
                    )
                }
                when (uiState.selectedTab) {
                    ArchiveTab.SavedHistory ->
                        savedHistoryItems(uiState = uiState, onIntent = onIntent)

                    ArchiveTab.Bookmarks ->
                        item(key = BOOKMARKS_KEY) {
                            Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
                                BurpRemoteSectionHeading(
                                    text = stringResource(R.string.archive_bookmarks_heading),
                                )
                                BurpRemoteEmptyState(
                                    headline = stringResource(R.string.archive_bookmarks_empty),
                                    detail = stringResource(R.string.archive_reason_bookmarks),
                                )
                            }
                        }

                    ArchiveTab.Screenshots ->
                        item(key = SCREENSHOTS_KEY) {
                            Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
                                BurpRemoteSectionHeading(
                                    text = stringResource(R.string.archive_screenshots_heading),
                                )
                                BurpRemoteEmptyState(
                                    headline = stringResource(R.string.archive_screenshots_empty),
                                    detail = stringResource(R.string.archive_reason_screenshots),
                                    actionText = stringResource(R.string.archive_screenshots_open_action),
                                    onAction = {
                                        haptics.tap()
                                        onIntent(ArchiveUserInterfaceIntent.OpenScreenshots)
                                    },
                                )
                            }
                        }
                }
            }
        }
    }
}

/**
 * 页签行用按钮拼：选中态是实心的，未选中是幽灵按钮，一眼能分辨，也不需要 Material 的 Tab。
 * 三个页签等宽，位置固定，手指换页时不必重新找目标。
 */
@Composable
private fun ArchiveTabRow(
    selectedTab: ArchiveTab,
    onSelectTab: (ArchiveTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
    ) {
        ArchiveTab.entries.forEach { tab ->
            Box(modifier = Modifier.weight(weight = 1f, fill = true)) {
                BurpRemoteButton(
                    text = stringResource(tab.labelResource),
                    onClick = { onSelectTab(tab) },
                    style =
                        if (tab == selectedTab) {
                            BurpRemoteButtonStyle.Primary
                        } else {
                            BurpRemoteButtonStyle.Ghost
                        },
                )
            }
        }
    }
}

private fun LazyListScope.savedHistoryItems(
    uiState: ArchiveUserInterfaceState,
    onIntent: (ArchiveUserInterfaceIntent) -> Unit,
) {
    val failureReasonResource = uiState.failureReasonResource
    when {
        failureReasonResource != null ->
            item(key = ERROR_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.archive_error_headline),
                    detail = stringResource(failureReasonResource),
                    actionText = stringResource(R.string.archive_error_action),
                    onAction = { onIntent(ArchiveUserInterfaceIntent.Refresh) },
                )
            }

        !uiState.hasLoaded ->
            item(key = LOADING_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.archive_loading_headline),
                    detail = stringResource(R.string.archive_loading_detail),
                )
            }

        uiState.savedRecords.isEmpty() ->
            item(key = SAVED_EMPTY_KEY) {
                Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
                    BurpRemoteSectionHeading(
                        text = stringResource(R.string.archive_saved_history_heading),
                    )
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.archive_saved_history_empty),
                        detail = stringResource(R.string.archive_saved_history_empty_detail),
                    )
                }
            }

        else -> {
            item(key = SAVED_HEADING_KEY) {
                BurpRemoteSectionHeading(
                    text = stringResource(R.string.archive_saved_history_heading),
                )
            }
            itemsIndexed(
                items = uiState.savedRecords,
                key = { _, record -> record.historyIdentifier.value },
            ) { index, record ->
                Box(
                    modifier =
                        Modifier
                            .animateItem()
                            .burpRemoteStaggeredEntry(index = index),
                ) {
                    SavedRecordCard(
                        record = record,
                        now = uiState.now,
                        onOpen = {
                            onIntent(
                                ArchiveUserInterfaceIntent.OpenSavedRecord(record.historyIdentifier.value),
                            )
                        },
                        onShare = {
                            onIntent(
                                ArchiveUserInterfaceIntent.ShareSavedRecord(record.historyIdentifier.value),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedRecordCard(
    record: HistoryRecord,
    now: Instant,
    onOpen: () -> Unit,
    onShare: () -> Unit,
) {
    val absentValue = stringResource(R.string.archive_absent_value)
    BurpRemoteCard(isInteractive = true, onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 「已归档」这一枚与实时历史的行区分开：这里的副本不会再被后续事件改写。
            BurpRemoteStatusPill(
                text = stringResource(R.string.archive_archived_pill),
                tone = BurpRemoteStatusTone.Neutral,
            )
            BurpRemoteStatusPill(
                text = record.method ?: absentValue,
                tone = methodToneOf(record.method),
            )
            BurpRemoteStatusPill(
                text = record.statusCode?.toString() ?: absentValue,
                tone = statusToneOf(record.statusCode),
            )
        }
        BurpRemoteTechnicalValue(text = targetTextOf(record = record, absentValue = absentValue))
        BurpRemoteTechnicalValue(
            text =
                record.savedAt?.let { savedAt -> RECORD_TIME_FORMATTER.format(savedAt) }
                    ?: RECORD_TIME_FORMATTER.format(record.occurredAt),
            label =
                stringResource(
                    R.string.archive_saved_at_label,
                    relativeTimeText(now = now, moment = record.savedAt ?: record.occurredAt),
                ),
        )
        BurpRemoteButton(
            text = stringResource(R.string.archive_action_share),
            onClick = onShare,
            style = BurpRemoteButtonStyle.Secondary,
        )
    }
}

/**
 * 方法色标按动词语义分档，与实时历史、拦截同一套：读类方法中性偏在线，写类方法警告，删除危险。
 */
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

private fun targetTextOf(
    record: HistoryRecord,
    absentValue: String,
): String {
    val host = record.host
    val path = record.path
    return when {
        host != null && path != null -> host + path
        host != null -> host
        path != null -> path
        else -> absentValue
    }
}

// 相对时间给人扫一眼「多久之前」；精确时刻是主文本，长按可复制。
@Composable
private fun relativeTimeText(
    now: Instant,
    moment: Instant,
): String {
    val resources = LocalContext.current.resources
    val elapsedSeconds = Duration.between(moment, now).seconds.coerceAtLeast(0L)
    return when {
        elapsedSeconds < JUST_NOW_SECONDS -> resources.getString(R.string.archive_relative_just_now)
        elapsedSeconds < SECONDS_PER_MINUTE ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.archive_relative_seconds,
                value = elapsedSeconds,
            )

        elapsedSeconds < SECONDS_PER_HOUR ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.archive_relative_minutes,
                value = elapsedSeconds / SECONDS_PER_MINUTE,
            )

        elapsedSeconds < SECONDS_PER_DAY ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.archive_relative_hours,
                value = elapsedSeconds / SECONDS_PER_HOUR,
            )

        else ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.archive_relative_days,
                value = elapsedSeconds / SECONDS_PER_DAY,
            )
    }
}

private fun quantityText(
    resources: Resources,
    pluralResource: Int,
    value: Long,
): String = resources.getQuantityString(pluralResource, value.toInt(), value.toInt())

@get:StringRes
internal val ArchiveTab.labelResource: Int
    get() =
        when (this) {
            ArchiveTab.SavedHistory -> R.string.archive_tab_saved_history
            ArchiveTab.Bookmarks -> R.string.archive_tab_bookmarks
            ArchiveTab.Screenshots -> R.string.archive_tab_screenshots
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

private const val JUST_NOW_SECONDS = 10L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_DAY = 86_400L

private const val TABS_KEY = "tabs"
private const val SAVED_HEADING_KEY = "saved-heading"
private const val SAVED_EMPTY_KEY = "saved-empty"
private const val BOOKMARKS_KEY = "bookmarks"
private const val SCREENSHOTS_KEY = "screenshots"
private const val LOADING_KEY = "loading"
private const val ERROR_KEY = "error"

// 时刻按设备时区与当前语言格式化；归档要能跟插件上的记录对上，因此精确到秒。
private val RECORD_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "已保存浅色", showBackground = true)
@Composable
private fun ArchiveScreenSavedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        ArchiveScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "已保存深色", showBackground = true)
@Composable
private fun ArchiveScreenSavedDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        ArchiveScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "书签浅色", showBackground = true)
@Composable
private fun ArchiveScreenBookmarksLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        ArchiveScreen(
            uiState = ArchiveUserInterfaceState(selectedTab = ArchiveTab.Bookmarks, hasLoaded = true),
            onIntent = {},
        )
    }
}

@Preview(name = "书签深色", showBackground = true)
@Composable
private fun ArchiveScreenBookmarksDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        ArchiveScreen(
            uiState = ArchiveUserInterfaceState(selectedTab = ArchiveTab.Bookmarks, hasLoaded = true),
            onIntent = {},
        )
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun populatedPreviewState(): ArchiveUserInterfaceState =
    ArchiveUserInterfaceState(
        savedRecords =
            listOf(
                previewRecord(method = "GET", path = "/api/login", statusCode = 200),
                previewRecord(method = "POST", path = "/api/user", statusCode = 403),
            ),
        now = Instant.parse("2026-09-13T08:10:00Z"),
        hasLoaded = true,
    )

private fun previewRecord(
    method: String,
    path: String,
    statusCode: Int,
): HistoryRecord =
    HistoryRecord(
        historyIdentifier = HistoryIdentifier(value = "$method $path"),
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
