package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemotePullToRefresh
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
 * 实时历史（plan §20）。
 *
 * 无状态：列表只显示元数据，正文按需另取；每一行以历史标识作稳定 key（rules.md §8.3），
 * 顺序变化时靠 [Modifier.animateItem] 平滑落位、靠错峰入场交代「这是同一批新记录」。
 * 行结构与拦截、归档三处保持同一套：方法色标 + 状态码色标在上，主机与路径等宽在下，
 * 时刻既给精确值（可长按复制）也给相对值（扫一眼就知道有多新）。
 *
 * 交互全在这一层：下拉刷新、点击进详情、长按复制技术值、触感分档。
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
                        horizontal = BurpRemoteSpacing.Large,
                        vertical = BurpRemoteSpacing.Large,
                    ),
                verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            ) {
                recordItems(uiState = uiState, onIntent = onIntent)
            }
        }
    }
}

/** 列表区的三态：等待、可重试的失败、要么空要么有内容。 */
private fun LazyListScope.recordItems(
    uiState: HistoryUserInterfaceState,
    onIntent: (HistoryUserInterfaceIntent) -> Unit,
) {
    val failureReasonResource = uiState.failureReasonResource
    when {
        failureReasonResource != null ->
            item(key = ERROR_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.history_error_headline),
                    detail = stringResource(failureReasonResource),
                    actionText = stringResource(R.string.history_error_action),
                    onAction = { onIntent(HistoryUserInterfaceIntent.Refresh) },
                )
            }

        !uiState.hasLoaded ->
            item(key = LOADING_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.history_loading_headline),
                    detail = stringResource(R.string.history_loading_detail),
                )
            }

        uiState.records.isEmpty() ->
            item(key = EMPTY_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.history_empty_headline),
                    detail = stringResource(R.string.history_empty_detail),
                )
            }

        else ->
            itemsIndexed(
                items = uiState.records,
                key = { _, record -> record.historyIdentifier.value },
            ) { index, record ->
                Box(
                    modifier =
                        Modifier
                            .animateItem()
                            .burpRemoteStaggeredEntry(index = index),
                ) {
                    HistoryRecordCard(
                        record = record,
                        now = uiState.now,
                        onOpen = {
                            onIntent(
                                HistoryUserInterfaceIntent.OpenHistoryRecord(
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
private fun HistoryRecordCard(
    record: HistoryRecord,
    now: Instant,
    onOpen: () -> Unit,
) {
    val absentValue = stringResource(R.string.history_absent_value)
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
        BurpRemoteTechnicalValue(text = targetTextOf(record = record, absentValue = absentValue))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
        ) {
            Box(modifier = Modifier.weight(weight = 1f, fill = true)) {
                BurpRemoteTechnicalValue(
                    text = responseLengthTextOf(record = record, absentValue = absentValue),
                    label = stringResource(R.string.history_response_length_label),
                )
            }
            Box(modifier = Modifier.weight(weight = 1f, fill = true)) {
                BurpRemoteTechnicalValue(
                    text = durationTextOf(record = record, absentValue = absentValue),
                    label = stringResource(R.string.history_duration_label),
                )
            }
        }
        BurpRemoteTechnicalValue(
            text = RECORD_TIME_FORMATTER.format(record.occurredAt),
            label = relativeTimeText(now = now, moment = record.occurredAt),
        )
    }
}

@Composable
private fun responseLengthTextOf(
    record: HistoryRecord,
    absentValue: String,
): String =
    record.responseLength?.let { responseLength ->
        stringResource(R.string.history_response_length_value, responseLength)
    } ?: absentValue

@Composable
private fun durationTextOf(
    record: HistoryRecord,
    absentValue: String,
): String =
    record.durationMilliseconds?.let { durationMilliseconds ->
        stringResource(R.string.history_duration_value, durationMilliseconds)
    } ?: absentValue

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

private const val ERROR_KEY = "error"
private const val LOADING_KEY = "loading"
private const val EMPTY_KEY = "empty"

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
