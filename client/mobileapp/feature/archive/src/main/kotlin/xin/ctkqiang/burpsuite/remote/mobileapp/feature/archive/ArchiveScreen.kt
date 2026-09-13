package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteErrorState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemotePullToRefresh
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSkeletonRow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteMotion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSizing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens
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
 * 因此那两个页签写明缺口，不摆假条目。页签做成等宽三段的分段控件——选中段是实心胶囊，
 * 指示器用 320ms 滑过去，切换时给一记选中触感，「换了一组数据」这件事于是有了交代。
 *
 * 整个页面只有一个滚动容器（[LazyColumn]），分段控件是它的第一项——把页签放在 LazyColumn 外面
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
                        horizontal = BurpRemoteSpacing.ScreenEdge,
                        vertical = BurpRemoteSpacing.ExtraLarge,
                    ),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraLarge),
            ) {
                item(key = TABS_KEY) {
                    ArchiveSegmentControl(
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
 * 分段控件：三段等宽，选中段是实心胶囊，指示器在 320ms 内滑过去。
 *
 * 用滑动指示器而不是三枚按钮切换，是为了让「当前在哪一段」这件事由位置本身说明，
 * 而不是靠哪一枚按钮底色变了去猜（plan §43）。
 */
@Composable
private fun ArchiveSegmentControl(
    selectedTab: ArchiveTab,
    onSelectTab: (ArchiveTab) -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val containerShape = RoundedCornerShape(BurpRemoteRadius.Capsule)
    val selectedIndex = ArchiveTab.entries.indexOf(selectedTab)

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(containerShape)
                .background(color = tokens.colourScheme.surfaceElevated, shape = containerShape)
                .border(width = 1.dp, color = tokens.colourScheme.outline, shape = containerShape)
                .padding(BurpRemoteSpacing.ExtraSmall),
    ) {
        val segmentWidth = (maxWidth - BurpRemoteSpacing.ExtraSmall * 2) / ArchiveTab.entries.size
        val indicatorOffset by
            animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec =
                    tween(
                        durationMillis = BurpRemoteMotion.DURATION_EMPHASISED,
                        easing = BurpRemoteMotion.EasingStandard,
                    ),
                label = "archive-segment-indicator",
            )

        Box(
            modifier =
                Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .height(BurpRemoteSizing.MinimumTouchTarget)
                    .clip(containerShape)
                    .background(color = tokens.colourScheme.accent, shape = containerShape),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            ArchiveTab.entries.forEach { tab ->
                Box(
                    modifier =
                        Modifier
                            .weight(weight = 1f, fill = true)
                            .height(BurpRemoteSizing.MinimumTouchTarget)
                            .clip(containerShape)
                            .clickable { onSelectTab(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    ScreenText(
                        text = stringResource(tab.labelResource),
                        style = tokens.typography.label,
                        colour =
                            if (tab == selectedTab) {
                                tokens.colourScheme.onAccent
                            } else {
                                tokens.colourScheme.contentSecondary
                            },
                        maxLines = 1,
                    )
                }
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
                BurpRemoteErrorState(
                    detail = stringResource(failureReasonResource),
                    onRetry = { onIntent(ArchiveUserInterfaceIntent.Refresh) },
                )
            }

        !uiState.hasLoaded ->
            item(key = LOADING_KEY) {
                BurpRemoteSkeletonRow()
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

/**
 * 已保存的一行：与实时历史的行共用同一套方法色标与状态胶囊，但归档语义单独说清楚。
 *
 * 「归档」这一枚与列表语义分开：这里的副本不会再被后续事件改写，因此不能长得像一条实时记录。
 */
@Composable
private fun SavedRecordCard(
    record: HistoryRecord,
    now: Instant,
    onOpen: () -> Unit,
    onShare: () -> Unit,
) {
    val absentValue = stringResource(R.string.archive_absent_value)
    val savedAt = record.savedAt ?: record.occurredAt

    BurpRemoteCard(isInteractive = true, onClick = onOpen) {
        RequestHeadlineRow(
            method = record.method,
            statusCode = record.statusCode,
            target = targetTextOf(record = record, absentValue = absentValue),
            absentValue = absentValue,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BurpRemoteStatusPill(
                text = stringResource(R.string.archive_archived_pill),
                tone = BurpRemoteStatusTone.Neutral,
            )
            Spacer(modifier = Modifier.weight(weight = 1f, fill = true))
            BurpRemoteButton(
                text = stringResource(R.string.archive_action_share),
                onClick = onShare,
                style = BurpRemoteButtonStyle.Secondary,
            )
        }
        BurpRemoteTechnicalValue(
            text = RECORD_TIME_FORMATTER.format(savedAt),
            label =
                stringResource(
                    R.string.archive_saved_at_label,
                    relativeTimeText(now = now, moment = savedAt),
                ),
        )
    }
}

/**
 * 列表共用的第一行：左方法色标、中目标（等宽）、右状态胶囊。
 *
 * 与实时历史、拦截是同一套排列，归档里的副本因此也能和实时记录逐行对照。
 */
@Composable
private fun RequestHeadlineRow(
    method: String?,
    statusCode: Int?,
    target: String,
    absentValue: String,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val methodTone = methodToneOf(method)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MethodMarker(tone = methodTone)
        ScreenText(
            text = method ?: absentValue,
            style = tokens.typography.technical,
            colour = toneColourOf(tone = methodTone, colourScheme = tokens.colourScheme),
            maxLines = 1,
        )
        ScreenText(
            text = target,
            style = tokens.typography.technical,
            colour = tokens.colourScheme.contentPrimary,
            modifier = Modifier.weight(weight = 1f, fill = true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BurpRemoteStatusPill(
            text = statusCode?.toString() ?: absentValue,
            tone = statusToneOf(statusCode),
        )
    }
}

/** 方法色标：一竖条按方法语义着色，与实时历史、拦截是同一个意思。 */
@Composable
private fun MethodMarker(tone: BurpRemoteStatusTone) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val shape = RoundedCornerShape(BurpRemoteRadius.Capsule)

    Box(
        modifier =
            Modifier
                .width(METHOD_MARKER_WIDTH)
                .height(METHOD_MARKER_HEIGHT)
                .clip(shape)
                .background(
                    color = toneColourOf(tone = tone, colourScheme = tokens.colourScheme),
                    shape = shape,
                ),
    )
}

// 设计系统只发布具名控件；列表内部的自由排版在这里用主题字阶直接渲染，色值仍取自主题。
@Composable
private fun ScreenText(
    text: String,
    style: TextStyle,
    colour: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = colour),
        maxLines = maxLines,
        overflow = overflow,
    )
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

/** 语气到语义色的映射：界面里所有色值都从主题取，不自己写色号。 */
private fun toneColourOf(
    tone: BurpRemoteStatusTone,
    colourScheme: BurpRemoteColourScheme,
): Color =
    when (tone) {
        BurpRemoteStatusTone.Neutral -> colourScheme.contentSecondary
        BurpRemoteStatusTone.Live -> colourScheme.success
        BurpRemoteStatusTone.Warning -> colourScheme.warning
        BurpRemoteStatusTone.Danger -> colourScheme.danger
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

private val METHOD_MARKER_WIDTH = 4.dp
private val METHOD_MARKER_HEIGHT = 20.dp

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

@Preview(name = "截图浅色", showBackground = true)
@Composable
private fun ArchiveScreenScreenshotsLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        ArchiveScreen(
            uiState = ArchiveUserInterfaceState(selectedTab = ArchiveTab.Screenshots, hasLoaded = true),
            onIntent = {},
        )
    }
}

@Preview(name = "截图深色", showBackground = true)
@Composable
private fun ArchiveScreenScreenshotsDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        ArchiveScreen(
            uiState = ArchiveUserInterfaceState(selectedTab = ArchiveTab.Screenshots, hasLoaded = true),
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
