package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteErrorState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItem
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteListItemGroup
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemotePullToRefresh
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSkeletonRow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.burpRemoteStaggeredEntry
import java.time.Duration
import java.time.Instant

/**
 * 实时历史（plan §20）。
 *
 * 无状态：列表只显示元数据，正文按需另取。
 *
 * 一屏记录是同一串历史里按时间排下来的相邻几条，因此整块共用一个描边容器、行间只画一条 1dp 分隔线；
 * 一行一张各自带描边的卡片读起来是「一堆互不相干的东西」，每张卡还要各撑一圈内边距，一屏只剩两三条。
 * 行内的三段固定为：方法色标、请求行（方法 + 主机 + 路径，等宽）、状态胶囊，第二行是相对时间。
 * 行内不再逐条摆技术值，也不再做行内的长按复制：整行的按下归「进详情」，再挂一个长按会与它抢同一串事件；
 * 精确时刻、长度与耗时都在详情页，那里仍然逐条可复制。
 *
 * 交互全在这一层：下拉刷新、点击整行进详情。
 */
@Composable
fun HistoryScreen(
    uiState: HistoryUserInterfaceState,
    onIntent: (HistoryUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        BurpRemotePullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onIntent(HistoryUserInterfaceIntent.Refresh) },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        horizontal = BurpRemoteSpacing.ScreenEdge,
                        vertical = BurpRemoteSpacing.ExtraLarge,
                    ),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ListItemGap),
            ) {
                recordItems(uiState = uiState, onIntent = onIntent)
            }
        }
    }
}

/**
 * 列表区的三态：等待、可重试的失败、要么空要么有内容。
 *
 * 还没读出来时给骨架行而不是一句「加载中」：列表的形状先落位，数据到了只是把灰块换成文字。
 */
private fun LazyListScope.recordItems(
    uiState: HistoryUserInterfaceState,
    onIntent: (HistoryUserInterfaceIntent) -> Unit,
) {
    val failureReasonResource = uiState.failureReasonResource
    when {
        failureReasonResource != null ->
            item(key = ERROR_KEY) {
                BurpRemoteErrorState(
                    detail = stringResource(failureReasonResource),
                    onRetry = { onIntent(HistoryUserInterfaceIntent.Refresh) },
                )
            }

        !uiState.hasLoaded ->
            items(
                count = SKELETON_ROW_COUNT,
                key = { index -> "$LOADING_KEY-$index" },
            ) { index ->
                Box(modifier = Modifier.burpRemoteStaggeredEntry(index = index)) {
                    BurpRemoteSkeletonRow()
                }
            }

        uiState.records.isEmpty() ->
            item(key = EMPTY_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.history_empty_headline),
                    detail = stringResource(R.string.history_empty_detail),
                )
            }

        else ->
            item(key = RECORDS_KEY) {
                Box(modifier = Modifier.animateItem()) {
                    HistoryRecordGroup(
                        records = uiState.records,
                        now = uiState.now,
                        onOpenRecord = { identifier ->
                            onIntent(HistoryUserInterfaceIntent.OpenHistoryRecord(identifier))
                        },
                    )
                }
            }
    }
}

/**
 * 一屏记录共用的分组容器：整组一张描边卡，行与行之间只画一条分隔线。
 *
 * 一屏记录互相之间的关系是「同一串历史里前后相邻的几条」，共用一张容器才读得成一条时间线；
 * 每行各自一张卡片时，这一层关系就没了，取而代之的是一堆各自独立的方框。
 *
 * 行内的错峰入场挂在每一行上，因此新记录仍然是一条条落下，而不是整组一起弹出来。
 *
 * @param records 这一屏仍在实时投影里的记录，按事件序号升序。
 * @param now 相对时间的参照点。
 * @param onOpenRecord 打开某条记录的详情；参数是这条记录的历史标识。
 */
@Composable
private fun HistoryRecordGroup(
    records: List<HistoryRecord>,
    now: Instant,
    onOpenRecord: (String) -> Unit,
) {
    val absentValue = stringResource(R.string.history_absent_value)

    BurpRemoteListItemGroup {
        records.forEachIndexed { index, record ->
            BurpRemoteListItem(
                title = requestLineOf(record = record, absentValue = absentValue),
                modifier = Modifier.burpRemoteStaggeredEntry(index = index),
                subtitle = relativeTimeText(now = now, moment = record.occurredAt),
                titleIsTechnical = true,
                leading = { MethodMarker(tone = methodToneOf(record.method)) },
                trailing = {
                    BurpRemoteStatusPill(
                        text = record.statusCode?.toString() ?: absentValue,
                        tone = statusToneOf(record.statusCode),
                    )
                },
                showsDivider = index != records.lastIndex,
                onClick = { onOpenRecord(record.historyIdentifier.value) },
            )
        }
    }
}

/** 方法色标：一竖条按方法语义着色，与拦截、归档两个列表是同一个意思。 */
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

/**
 * 方法色标按动词语义分档：读类方法不改服务端状态，写类方法会改状态，删除不可逆。
 * 三处列表（实时历史、拦截、归档）用同一套分档，颜色因此在三个页面里有同一个含义。
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

// 相对时间给人扫一眼「多久之前」，行内第二行只放它；精确时刻在详情页。
@Composable
private fun relativeTimeText(
    now: Instant,
    moment: Instant,
): String {
    val resources = LocalContext.current.resources
    val elapsedSeconds = Duration.between(moment, now).seconds.coerceAtLeast(0L)
    return when {
        elapsedSeconds < JUST_NOW_SECONDS -> resources.getString(R.string.history_relative_just_now)
        elapsedSeconds < SECONDS_PER_MINUTE ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.history_relative_seconds,
                value = elapsedSeconds,
            )

        elapsedSeconds < SECONDS_PER_HOUR ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.history_relative_minutes,
                value = elapsedSeconds / SECONDS_PER_MINUTE,
            )

        elapsedSeconds < SECONDS_PER_DAY ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.history_relative_hours,
                value = elapsedSeconds / SECONDS_PER_HOUR,
            )

        else ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.history_relative_days,
                value = elapsedSeconds / SECONDS_PER_DAY,
            )
    }
}

private fun quantityText(
    resources: Resources,
    pluralResource: Int,
    value: Long,
): String = resources.getQuantityString(pluralResource, value.toInt(), value.toInt())

private val METHOD_MARKER_WIDTH = 4.dp
private val METHOD_MARKER_HEIGHT = 20.dp

private const val SKELETON_ROW_COUNT = 4

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

private const val ERROR_KEY = "error"
private const val LOADING_KEY = "loading"
private const val EMPTY_KEY = "empty"

// 整块记录是一个懒惰列表项：一组的描边与底色只有一份，行是它内部的几行。
private const val RECORDS_KEY = "records"

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "有记录浅色", showBackground = true)
@Composable
private fun HistoryScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "有记录深色", showBackground = true)
@Composable
private fun HistoryScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "读取中浅色", showBackground = true)
@Composable
private fun HistoryScreenLoadingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryScreen(uiState = HistoryUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "读取中深色", showBackground = true)
@Composable
private fun HistoryScreenLoadingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryScreen(uiState = HistoryUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "空列表浅色", showBackground = true)
@Composable
private fun HistoryScreenEmptyLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryScreen(uiState = HistoryUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "空列表深色", showBackground = true)
@Composable
private fun HistoryScreenEmptyDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryScreen(uiState = HistoryUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "读取失败浅色", showBackground = true)
@Composable
private fun HistoryScreenFailedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryScreen(
            uiState =
                HistoryUserInterfaceState(
                    hasLoaded = true,
                    failureReasonResource = R.string.history_error_read_failed,
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "读取失败深色", showBackground = true)
@Composable
private fun HistoryScreenFailedDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryScreen(
            uiState =
                HistoryUserInterfaceState(
                    hasLoaded = true,
                    failureReasonResource = R.string.history_error_read_failed,
                ),
            onIntent = {},
        )
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun populatedPreviewState(): HistoryUserInterfaceState =
    HistoryUserInterfaceState(
        records =
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
