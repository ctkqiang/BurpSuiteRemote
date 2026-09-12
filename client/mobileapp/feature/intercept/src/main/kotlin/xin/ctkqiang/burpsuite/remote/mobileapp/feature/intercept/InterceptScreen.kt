package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import android.content.res.Resources
import androidx.annotation.StringRes
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
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState
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
 * 拦截队列（plan §43 的 Live/Intercept）。
 *
 * 无状态：只显示投影，每一行以拦截标识作稳定 key。队列的看点全在「这条现在是什么状态」，
 * 因此状态做成带语义色的标签；行结构与实时历史、归档保持同一套，方法也用同一种色标，
 * 于是三个列表在配色与节奏上是一个产品，而不是三张各写各的表。
 */
@Composable
fun InterceptScreen(
    uiState: InterceptUserInterfaceState,
    onIntent: (InterceptUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        BurpRemotePullToRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onIntent(InterceptUserInterfaceIntent.Refresh) },
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
    uiState: InterceptUserInterfaceState,
    onIntent: (InterceptUserInterfaceIntent) -> Unit,
) {
    val failureReasonResource = uiState.failureReasonResource
    when {
        failureReasonResource != null ->
            item(key = ERROR_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.intercept_error_headline),
                    detail = stringResource(failureReasonResource),
                    actionText = stringResource(R.string.intercept_error_action),
                    onAction = { onIntent(InterceptUserInterfaceIntent.Refresh) },
                )
            }

        !uiState.hasLoaded ->
            item(key = LOADING_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.intercept_loading_headline),
                    detail = stringResource(R.string.intercept_loading_detail),
                )
            }

        uiState.records.isEmpty() ->
            item(key = EMPTY_KEY) {
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.intercept_empty_headline),
                    detail = stringResource(R.string.intercept_empty_detail),
                )
            }

        else ->
            itemsIndexed(
                items = uiState.records,
                key = { _, record -> record.interceptIdentifier.value },
            ) { index, record ->
                Box(
                    modifier =
                        Modifier
                            .animateItem()
                            .burpRemoteStaggeredEntry(index = index),
                ) {
                    InterceptRecordCard(
                        record = record,
                        now = uiState.now,
                        onOpen = {
                            onIntent(
                                InterceptUserInterfaceIntent.OpenInterceptRecord(
                                    record.interceptIdentifier.value,
                                ),
                            )
                        },
                    )
                }
            }
    }
}

@Composable
private fun InterceptRecordCard(
    record: InterceptRecord,
    now: Instant,
    onOpen: () -> Unit,
) {
    val absentValue = stringResource(R.string.intercept_absent_value)
    BurpRemoteCard(isInteractive = true, onClick = onOpen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BurpRemoteStatusPill(
                text = stringResource(record.state.labelResource),
                tone = toneOf(state = record.state),
            )
            BurpRemoteStatusPill(
                text = record.method ?: absentValue,
                tone = methodToneOf(record.method),
            )
        }
        BurpRemoteTechnicalValue(text = targetTextOf(record = record, absentValue = absentValue))
        BurpRemoteTechnicalValue(
            text = RECORD_TIME_FORMATTER.format(record.updatedAt),
            label = relativeTimeText(now = now, moment = record.updatedAt),
        )
    }
}

/**
 * 方法色标按动词语义分档，与实时历史、归档同一套：读类方法中性偏在线，写类方法警告，
 * 删除危险。颜色在三个列表里因此是同一个意思。
 */
private fun methodToneOf(method: String?): BurpRemoteStatusTone =
    when (method?.uppercase()) {
        METHOD_GET, METHOD_HEAD, METHOD_OPTIONS -> BurpRemoteStatusTone.Live
        METHOD_POST, METHOD_PUT, METHOD_PATCH -> BurpRemoteStatusTone.Warning
        METHOD_DELETE -> BurpRemoteStatusTone.Danger
        else -> BurpRemoteStatusTone.Neutral
    }

/** 状态配色：等决定的最需要被看见，已放行是正常流，已丢弃就安静下来。 */
private fun toneOf(state: InterceptState): BurpRemoteStatusTone =
    when (state) {
        InterceptState.Pending -> BurpRemoteStatusTone.Warning
        InterceptState.Modified -> BurpRemoteStatusTone.Warning
        InterceptState.Forwarded -> BurpRemoteStatusTone.Live
        InterceptState.Dropped -> BurpRemoteStatusTone.Neutral
    }

private fun targetTextOf(
    record: InterceptRecord,
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
        elapsedSeconds < JUST_NOW_SECONDS -> resources.getString(R.string.intercept_relative_just_now)
        elapsedSeconds < SECONDS_PER_MINUTE ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.intercept_relative_seconds,
                value = elapsedSeconds,
            )

        elapsedSeconds < SECONDS_PER_HOUR ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.intercept_relative_minutes,
                value = elapsedSeconds / SECONDS_PER_MINUTE,
            )

        elapsedSeconds < SECONDS_PER_DAY ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.intercept_relative_hours,
                value = elapsedSeconds / SECONDS_PER_HOUR,
            )

        else ->
            quantityText(
                resources = resources,
                pluralResource = R.plurals.intercept_relative_days,
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
internal val InterceptState.labelResource: Int
    get() =
        when (this) {
            InterceptState.Pending -> R.string.intercept_state_pending
            InterceptState.Modified -> R.string.intercept_state_modified
            InterceptState.Forwarded -> R.string.intercept_state_forwarded
            InterceptState.Dropped -> R.string.intercept_state_dropped
        }

/** 还在队列里等决定的状态；放行与丢弃是终态（plan §24）。 */
internal val InterceptState.isWaiting: Boolean
    get() = this == InterceptState.Pending || this == InterceptState.Modified

// 时刻按设备时区与当前语言格式化；队列里两次操作可能只差几秒，因此带秒。
private val RECORD_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

private const val METHOD_GET = "GET"
private const val METHOD_HEAD = "HEAD"
private const val METHOD_OPTIONS = "OPTIONS"
private const val METHOD_POST = "POST"
private const val METHOD_PUT = "PUT"
private const val METHOD_PATCH = "PATCH"
private const val METHOD_DELETE = "DELETE"

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
private fun InterceptScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        InterceptScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "有记录深色", showBackground = true)
@Composable
private fun InterceptScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        InterceptScreen(uiState = populatedPreviewState(), onIntent = {})
    }
}

@Preview(name = "空队列浅色", showBackground = true)
@Composable
private fun InterceptScreenEmptyLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        InterceptScreen(uiState = InterceptUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "空队列深色", showBackground = true)
@Composable
private fun InterceptScreenEmptyDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        InterceptScreen(uiState = InterceptUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun populatedPreviewState(): InterceptUserInterfaceState =
    InterceptUserInterfaceState(
        records =
            listOf(
                previewRecord(state = InterceptState.Pending, method = "POST", path = "/api/user"),
                previewRecord(state = InterceptState.Forwarded, method = "GET", path = "/api/admin"),
            ),
        now = Instant.parse("2026-09-13T08:10:00Z"),
        hasLoaded = true,
    )

private fun previewRecord(
    state: InterceptState,
    method: String,
    path: String,
): InterceptRecord =
    InterceptRecord(
        interceptIdentifier = InterceptIdentifier(value = "$method $path"),
        sequenceNumber = 5002L,
        createdAt = Instant.parse("2026-09-13T08:00:00Z"),
        updatedAt = Instant.parse("2026-09-13T08:00:01Z"),
        state = state,
        host = "api.example.com",
        method = method,
        path = path,
    )
