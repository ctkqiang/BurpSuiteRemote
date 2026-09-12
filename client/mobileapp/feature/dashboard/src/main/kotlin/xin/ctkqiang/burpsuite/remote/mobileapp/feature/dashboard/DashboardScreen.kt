package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

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
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ConnectionStateIndicator
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.SectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemotePullToRefresh
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.burpRemoteStaggeredEntry
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 主面板（plan §44）。
 *
 * 无状态：状态由外面传进来，用户意图往外抛。赏金猎人第一眼看这一屏，因此顺序固定为
 * 「连上没有 → 目标是谁 → 三路计数 → 从哪进实时区 → 最近几个请求」：先给结论，再给入口。
 *
 * 扫码入口归装配层顶栏（plan §55），内容区不再重复一个扫码按钮；[onOpenPairingScanner] 只作
 * 兼容壳层旧接线用，界面不拿它画控件。
 *
 * @param onOpenPairingScanner 壳层保留的扫码回调；内容层不再有对应的控件。
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUserInterfaceState,
    onIntent: (DashboardUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenPairingScanner: (() -> Unit)? = null,
) {
    val refresh = { onIntent(DashboardUserInterfaceIntent.Refresh) }

    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供（内边距也由它算给内容区），各屏不再画第二层标题。
        BurpRemotePullToRefresh(isRefreshing = uiState.isRefreshing, onRefresh = refresh) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        horizontal = BurpRemoteSpacing.Large,
                        vertical = BurpRemoteSpacing.Large,
                    ),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
            ) {
                item(key = CONNECTION_KEY) {
                    ConnectionSection(
                        uiState = uiState,
                        modifier = Modifier.burpRemoteStaggeredEntry(index = CONNECTION_ENTRY_INDEX),
                    )
                }
                item(key = STATISTICS_KEY) {
                    StatisticsSection(
                        uiState = uiState,
                        modifier = Modifier.burpRemoteStaggeredEntry(index = STATISTICS_ENTRY_INDEX),
                    )
                }
                item(key = ACTIONS_HEADING_KEY) {
                    BurpRemoteSectionHeading(text = stringResource(R.string.dashboard_actions_heading))
                }
                item(key = ACTIONS_KEY) {
                    LiveEntryAction(
                        onEntry = { intent ->
                            onIntent(intent)
                        },
                        modifier = Modifier.burpRemoteStaggeredEntry(index = ACTIONS_ENTRY_INDEX),
                    )
                }
                item(key = RECENT_HEADING_KEY) {
                    BurpRemoteSectionHeading(text = stringResource(R.string.dashboard_recent_heading))
                }
                recentRequestItems(
                    uiState = uiState,
                    onIntent = onIntent,
                )
            }
        }
    }
}

/**
 * 连接状态：这一屏的第一个结论。
 *
 * 只写真实存在的事实——会话状态、目标主机、最近一次观测时刻；延迟与同步时刻客户端都还没有
 * 端口，因此不摆一个恒为空的数字占位。
 */
@Composable
private fun ConnectionSection(
    uiState: DashboardUserInterfaceState,
    modifier: Modifier = Modifier,
) {
    val absentValue = stringResource(R.string.dashboard_absent_value)
    val latestRecord = uiState.recentRecords.firstOrNull()

    Box(modifier = modifier) {
        BurpRemoteCard {
            // LIVE/OFFLINE 由装配层顶栏常驻显示，内容区不再重复一枚；这里给的是更细的会话状态。
            ConnectionStateIndicator(connectionState = uiState.connectionState)
            BurpRemoteTechnicalValue(
                text = uiState.targetHost ?: absentValue,
                label = stringResource(R.string.dashboard_target_host_label),
            )
            if (latestRecord != null) {
                BurpRemoteTechnicalValue(
                    text = RECORD_TIME_FORMATTER.format(latestRecord.occurredAt),
                    label =
                        stringResource(
                            R.string.dashboard_last_observed_label,
                            relativeTimeText(now = uiState.now, moment = latestRecord.occurredAt),
                        ),
                )
            } else {
                BurpRemoteTechnicalValue(
                    text = absentValue,
                    label = stringResource(R.string.dashboard_last_observed_label, absentValue),
                )
            }
        }
    }
}

/** 一行三项统计：读不出结果时显示占位符，而不是一个会被误读成事实的 0。 */
@Composable
private fun StatisticsSection(
    uiState: DashboardUserInterfaceState,
    modifier: Modifier = Modifier,
) {
    val absentValue = stringResource(R.string.dashboard_absent_value)

    Box(modifier = modifier) {
        BurpRemoteCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            ) {
                StatisticValue(
                    value = uiState.liveRequestCount.takeIf { uiState.hasLoaded }?.toString() ?: absentValue,
                    labelResource = R.string.dashboard_live_request_label,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                )
                StatisticValue(
                    value = uiState.interceptedCount.takeIf { uiState.hasLoaded }?.toString() ?: absentValue,
                    labelResource = R.string.dashboard_intercepted_label,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                )
                StatisticValue(
                    value = uiState.savedCount.takeIf { uiState.hasLoaded }?.toString() ?: absentValue,
                    labelResource = R.string.dashboard_saved_label,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                )
            }
        }
    }
}

@Composable
private fun StatisticValue(
    value: String,
    @StringRes labelResource: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        BurpRemoteTechnicalValue(text = value, label = stringResource(labelResource))
    }
}

/** 三个主入口：每张卡写清它通向什么，用户不用靠猜再点进去。 */
@Composable
private fun LiveEntryAction(
    onEntry: (DashboardUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
    ) {
        EntryActionCard(
            titleResource = R.string.dashboard_entry_history,
            descriptionResource = R.string.dashboard_entry_history_detail,
            onClick = { onEntry(DashboardUserInterfaceIntent.OpenLiveHistory) },
        )
        EntryActionCard(
            titleResource = R.string.dashboard_entry_intercept,
            descriptionResource = R.string.dashboard_entry_intercept_detail,
            onClick = { onEntry(DashboardUserInterfaceIntent.OpenLiveIntercept) },
        )
        EntryActionCard(
            titleResource = R.string.dashboard_entry_repeater,
            descriptionResource = R.string.dashboard_entry_repeater_detail,
            onClick = { onEntry(DashboardUserInterfaceIntent.OpenLiveRepeater) },
        )
    }
}

@Composable
private fun EntryActionCard(
    @StringRes titleResource: Int,
    @StringRes descriptionResource: Int,
    onClick: () -> Unit,
) {
    BurpRemoteCard(isInteractive = true, onClick = onClick) {
        SectionHeading(titleResource = titleResource)
        EmptyStateText(messageResource = descriptionResource)
    }
}

/**
 * 最近请求的三态。
 *
 * 失败与空是两码事：失败给可重试的错误态，空给「为什么空、下一步做什么」，两者都不摆假条目
 * （rules.md §5.1）。
 */
private fun LazyListScope.recentRequestItems(
    uiState: DashboardUserInterfaceState,
    onIntent: (DashboardUserInterfaceIntent) -> Unit,
) {
    val failureReasonResource = uiState.failureReasonResource
    when {
        failureReasonResource != null ->
            item(key = ERROR_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.dashboard_error_headline),
                    detail = stringResource(failureReasonResource),
                    actionText = stringResource(R.string.dashboard_error_action),
                    onAction = { onIntent(DashboardUserInterfaceIntent.Refresh) },
                )
            }

        !uiState.hasLoaded ->
            item(key = LOADING_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.dashboard_loading_headline),
                    detail = stringResource(R.string.dashboard_loading_detail),
                )
            }

        uiState.recentRecords.isEmpty() ->
            item(key = RECENT_EMPTY_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(recentEmptyHeadlineResource(isLive = uiState.isLive)),
                    detail = stringResource(recentEmptyDetailResource(isLive = uiState.isLive)),
                )
            }

        else ->
            itemsIndexed(
                items = uiState.recentRecords,
                key = { _, record -> record.historyIdentifier.value },
            ) { index, record ->
                Box(
                    modifier =
                        Modifier
                            .animateItem()
                            .burpRemoteStaggeredEntry(index = index),
                ) {
                    RecentRequestCard(
                        record = record,
                        now = uiState.now,
                        onOpen = {
                            onIntent(
                                DashboardUserInterfaceIntent.OpenHistoryRecord(
                                    record.historyIdentifier.value,
                                ),
                            )
                        },
                    )
                }
            }
    }
}

@Composable
private fun RecentRequestCard(
    record: HistoryRecord,
    now: Instant,
    onOpen: () -> Unit,
) {
    val absentValue = stringResource(R.string.dashboard_absent_value)
    BurpRemoteCard(isInteractive = true, onClick = onOpen) {
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
        }
        // 主机与路径合成一个可复制的技术值：目标一眼看清，长按即可整条拿走。
        BurpRemoteTechnicalValue(text = targetTextOf(record = record, absentValue = absentValue))
        BurpRemoteTechnicalValue(
            text = RECORD_TIME_FORMATTER.format(record.occurredAt),
            label = relativeTimeText(now = now, moment = record.occurredAt),
        )
    }
}

/**
 * 方法色标按动词语义分档：读类方法不改服务端状态，保持中性偏在线；写类方法会改状态，标成警告；
 * 删除是不可逆的，标成危险。这样一屏扫下来，危险动作先跳出来。
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

/** 会话状态配色与连接屏一致：已连接是正常流，故障态走危险色。 */
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
        elapsedSeconds < JUST_NOW_SECONDS -> resources.getString(R.string.dashboard_relative_just_now)
        elapsedSeconds < SECONDS_PER_MINUTE ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.dashboard_relative_seconds,
                value = elapsedSeconds,
            )

        elapsedSeconds < SECONDS_PER_HOUR ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.dashboard_relative_minutes,
                value = elapsedSeconds / SECONDS_PER_MINUTE,
            )

        elapsedSeconds < SECONDS_PER_DAY ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.dashboard_relative_hours,
                value = elapsedSeconds / SECONDS_PER_HOUR,
            )

        else ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.dashboard_relative_days,
                value = elapsedSeconds / SECONDS_PER_DAY,
            )
    }
}

private fun quantityText(
    resources: Resources,
    pluralResource: Int,
    value: Long,
): String = resources.getQuantityString(pluralResource, value.toInt(), value.toInt())

private fun recentEmptyHeadlineResource(isLive: Boolean): Int =
    if (isLive) R.string.dashboard_recent_empty_live_headline else R.string.dashboard_recent_empty_offline_headline

private fun recentEmptyDetailResource(isLive: Boolean): Int =
    if (isLive) R.string.dashboard_recent_empty_live_detail else R.string.dashboard_recent_empty_offline_detail

// 时刻按设备时区与当前语言格式化；列表要能跟插件上的记录对上，因此精确到秒。
private val RECORD_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

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

private const val CONNECTION_ENTRY_INDEX = 0
private const val STATISTICS_ENTRY_INDEX = 1
private const val ACTIONS_ENTRY_INDEX = 2

private const val CONNECTION_KEY = "connection"
private const val STATISTICS_KEY = "statistics"
private const val ACTIONS_HEADING_KEY = "actions-heading"
private const val ACTIONS_KEY = "actions"
private const val RECENT_HEADING_KEY = "recent-heading"
private const val RECENT_EMPTY_KEY = "recent-empty"
private const val LOADING_KEY = "loading"
private const val ERROR_KEY = "error"

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "在线浅色", showBackground = true)
@Composable
private fun DashboardScreenLiveLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        DashboardScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "在线深色", showBackground = true)
@Composable
private fun DashboardScreenLiveDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        DashboardScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "离线空态浅色", showBackground = true)
@Composable
private fun DashboardScreenOfflineLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        DashboardScreen(uiState = DashboardUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "离线空态深色", showBackground = true)
@Composable
private fun DashboardScreenOfflineDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        DashboardScreen(uiState = DashboardUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "读取失败浅色", showBackground = true)
@Composable
private fun DashboardScreenFailedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        DashboardScreen(uiState = failedPreviewState(), onIntent = {})
    }
}

@Preview(name = "读取失败深色", showBackground = true)
@Composable
private fun DashboardScreenFailedDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        DashboardScreen(uiState = failedPreviewState(), onIntent = {})
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
                previewRecord(method = "DELETE", path = "/api/admin", statusCode = 500),
            ),
        now = Instant.parse("2026-09-13T08:10:00Z"),
        hasLoaded = true,
    )

private fun failedPreviewState(): DashboardUserInterfaceState =
    DashboardUserInterfaceState(
        hasLoaded = true,
        failureReasonResource = R.string.dashboard_error_read_failed,
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
