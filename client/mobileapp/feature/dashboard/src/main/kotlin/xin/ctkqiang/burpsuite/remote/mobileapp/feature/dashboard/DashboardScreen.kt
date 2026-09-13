package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LiveOfflineBadge
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteErrorState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItem
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItemGroup
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemotePullToRefresh
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSkeletonRow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
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
 * 主面板（plan §44）。
 *
 * 无状态：状态由外面传进来，用户意图往外抛。赏金猎人第一眼看这一屏，因此顺序固定为
 * 「会话状态 → 三路计数 → 最近几个请求」：先给结论，再给规模，最后给证据。
 *
 * 这里不摆「实时工作区」那张三入口卡片：底栏已经有一个实时分区，同一条路在内容区再画一遍，
 * 用户得先判断「这两个实时是不是同一个」。要进历史、拦截或重放，走底栏那一个入口就够。
 *
 * 最近请求是一整块带分隔线的列表，而不是一叠各自独立的卡片：一屏五张卡片读起来是五件互不相干的事，
 * 实际上它们是一串按时间排下来的记录。
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUserInterfaceState,
    onIntent: (DashboardUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refresh = { onIntent(DashboardUserInterfaceIntent.Refresh) }

    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供（内边距也由它算给内容区），各屏不再画第二层标题。
        BurpRemotePullToRefresh(isRefreshing = uiState.isRefreshing, onRefresh = refresh) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        horizontal = BurpRemoteSpacing.ScreenEdge,
                        vertical = BurpRemoteSpacing.ExtraLarge,
                    ),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraLarge),
            ) {
                item(key = SESSION_KEY) {
                    Box(modifier = Modifier.burpRemoteStaggeredEntry(index = SESSION_ENTRY_INDEX)) {
                        ConnectionStatusCard(uiState = uiState)
                    }
                }
                item(key = STATISTICS_KEY) {
                    Box(modifier = Modifier.burpRemoteStaggeredEntry(index = STATISTICS_ENTRY_INDEX)) {
                        StatisticsCard(uiState = uiState)
                    }
                }
                item(key = RECENT_HEADING_KEY) {
                    BurpRemoteSectionHeading(text = stringResource(R.string.dashboard_recent_heading))
                }
                item(key = RECENT_KEY) {
                    Box(modifier = Modifier.burpRemoteStaggeredEntry(index = RECENT_ENTRY_INDEX)) {
                        RecentRequests(uiState = uiState, onIntent = onIntent)
                    }
                }
            }
        }
    }
}

/**
 * 会话状态卡：这一屏的第一个结论。
 *
 * 只写真实存在的事实——LIVE/OFFLINE、目标主机、最近一次观测时刻；延迟与同步时刻客户端都还没有
 * 端口，因此不摆一个恒为空的数字占位（rules.md §5.1）。主机与时刻都是等宽可复制技术值。
 */
@Composable
private fun ConnectionStatusCard(uiState: DashboardUserInterfaceState) {
    val absentValue = stringResource(R.string.dashboard_absent_value)
    val latestRecord = uiState.recentRecords.firstOrNull()

    BurpRemoteCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveOfflineBadge(connectionState = uiState.connectionState)
        }
        CardDivider()
        BurpRemoteTechnicalValue(
            text = uiState.targetHost ?: absentValue,
            label = stringResource(R.string.dashboard_target_host_label),
        )
        BurpRemoteTechnicalValue(
            text = latestRecord?.let { record -> RECORD_TIME_FORMATTER.format(record.occurredAt) } ?: absentValue,
            label =
                stringResource(
                    R.string.dashboard_last_observed_label,
                    latestRecord?.let { record -> relativeTimeText(now = uiState.now, moment = record.occurredAt) }
                        ?: absentValue,
                ),
        )
    }
}

/**
 * 一行三项计数：标签在上、数字在下。
 *
 * 标签在上是有意的——先看到的是「这是哪个指标」，再看到数字，扫一眼就知道 0 属于哪一项；
 * 反过来读时，三个数字会先在眼前连成一片，再看标签才知道它们各是什么。
 *
 * 读不出结果时显示占位符，而不是一个会被误读成事实的 0。
 */
@Composable
private fun StatisticsCard(uiState: DashboardUserInterfaceState) {
    val absentValue = stringResource(R.string.dashboard_absent_value)

    BurpRemoteCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
        ) {
            StatisticCell(
                value = uiState.liveRequestCount.takeIf { uiState.hasLoaded }?.toString() ?: absentValue,
                labelResource = R.string.dashboard_live_request_label,
                modifier = Modifier.weight(weight = 1f, fill = true),
            )
            StatisticCell(
                value = uiState.interceptedCount.takeIf { uiState.hasLoaded }?.toString() ?: absentValue,
                labelResource = R.string.dashboard_intercepted_label,
                modifier = Modifier.weight(weight = 1f, fill = true),
            )
            StatisticCell(
                value = uiState.savedCount.takeIf { uiState.hasLoaded }?.toString() ?: absentValue,
                labelResource = R.string.dashboard_saved_label,
                modifier = Modifier.weight(weight = 1f, fill = true),
            )
        }
    }
}

@Composable
private fun StatisticCell(
    value: String,
    @StringRes labelResource: Int,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraSmall),
    ) {
        ScreenText(
            text = stringResource(labelResource),
            style = tokens.typography.caption,
            colour = tokens.colourScheme.contentSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ScreenText(
            text = value,
            style = tokens.typography.display,
            colour = tokens.colourScheme.contentPrimary,
            maxLines = 1,
        )
    }
}

/**
 * 最近请求的三态。
 *
 * 失败与空是两码事：失败给可重试的错误态，空给「为什么空、下一步做什么」，两者都不摆假条目
 * （rules.md §5.1）。还没读出来时给骨架行，让列表的形状先落位，而不是一句「加载中」。
 */
@Composable
private fun RecentRequests(
    uiState: DashboardUserInterfaceState,
    onIntent: (DashboardUserInterfaceIntent) -> Unit,
) {
    val failureReasonResource = uiState.failureReasonResource
    when {
        failureReasonResource != null ->
            BurpRemoteErrorState(
                detail = stringResource(failureReasonResource),
                onRetry = { onIntent(DashboardUserInterfaceIntent.Refresh) },
            )

        !uiState.hasLoaded ->
            Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ListItemGap)) {
                repeat(times = SKELETON_ROW_COUNT) { index ->
                    Box(modifier = Modifier.burpRemoteStaggeredEntry(index = index)) {
                        BurpRemoteSkeletonRow()
                    }
                }
            }

        uiState.recentRecords.isEmpty() ->
            BurpRemoteEmptyState(
                headline = stringResource(recentEmptyHeadlineResource(isLive = uiState.isLive)),
                detail = stringResource(recentEmptyDetailResource(isLive = uiState.isLive)),
            )

        else ->
            BurpRemoteListItemGroup {
                val absentValue = stringResource(R.string.dashboard_absent_value)
                uiState.recentRecords.forEachIndexed { index, record ->
                    BurpRemoteListItem(
                        title = requestLineOf(record = record, absentValue = absentValue),
                        subtitle = relativeTimeText(now = uiState.now, moment = record.occurredAt),
                        titleIsTechnical = true,
                        leading = { MethodMarker(tone = methodToneOf(record.method)) },
                        trailing = {
                            BurpRemoteStatusPill(
                                text = record.statusCode?.toString() ?: absentValue,
                                tone = statusToneOf(record.statusCode),
                            )
                        },
                        showsDivider = index != uiState.recentRecords.lastIndex,
                        onClick = {
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

/** 卡片内部的分段线；与列表行用的那条同宽同色，视觉上属于同一套。 */
@Composable
private fun CardDivider() {
    val tokens = LocalBurpRemoteDesignTokens.current

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(BurpRemoteSizing.Divider),
    ) {
        val strokeWidth = size.height
        drawLine(
            color = tokens.colourScheme.outline,
            start = Offset(0f, strokeWidth / 2f),
            end = Offset(size.width, strokeWidth / 2f),
            strokeWidth = strokeWidth,
        )
    }
}

/** 方法色标：一竖条按方法语义着色，扫下来时同一类方法落在同一个颜色上。 */
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

// 设计系统只发布具名控件；卡片内部的自由排版在这里用主题字阶直接渲染，色值仍取自主题。
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
        style = style.copy(color = colour),
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
    )
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

/**
 * 请求行：方法在前，后面跟主机与路径。
 *
 * 一行里同时给出方法、主机与路径，是因为列表行只剩一行标题——少了主机就没法在同一屏上区分
 * 两个相同路径的请求。缺失的段落在原处留空位，不把 `null` 拼进字符串。
 */
private fun requestLineOf(
    record: HistoryRecord,
    absentValue: String,
): String {
    val method = record.method ?: absentValue
    val target = targetTextOf(record = record, absentValue = absentValue)
    return "$method $target"
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

// 相对时间给人扫一眼「多久之前」；精确时刻在主卡片上是可复制的技术值。
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

private val METHOD_MARKER_WIDTH = 4.dp
private val METHOD_MARKER_HEIGHT = 20.dp

private const val SKELETON_ROW_COUNT = 3

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

private const val SESSION_ENTRY_INDEX = 0
private const val STATISTICS_ENTRY_INDEX = 1
private const val RECENT_ENTRY_INDEX = 2

private const val SESSION_KEY = "connection"
private const val STATISTICS_KEY = "statistics"
private const val RECENT_HEADING_KEY = "recent-heading"
private const val RECENT_KEY = "recent"

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

@Preview(name = "读取中浅色", showBackground = true)
@Composable
private fun DashboardScreenLoadingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        DashboardScreen(uiState = DashboardUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "读取中深色", showBackground = true)
@Composable
private fun DashboardScreenLoadingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        DashboardScreen(uiState = DashboardUserInterfaceState(), onIntent = {})
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
