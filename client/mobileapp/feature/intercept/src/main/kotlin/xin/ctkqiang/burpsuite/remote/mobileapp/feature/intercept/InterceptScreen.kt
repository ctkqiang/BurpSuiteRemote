package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteErrorState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemotePullToRefresh
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSkeletonRow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteColourScheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteRadius
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
 * 拦截队列（plan §43 的 Live/Intercept）。
 *
 * 无状态：只显示投影，每一行以拦截标识作稳定 key。队列的看点全在「这条现在是什么状态」，
 * 因此状态做成带语义色的胶囊；行结构与实时历史、归档保持同一套，方法用同一种色标，
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
 * 还没读出来时给骨架行：队列的形状先落位，数据到了只是把灰块换成文字。
 */
private fun LazyListScope.recordItems(
    uiState: InterceptUserInterfaceState,
    onIntent: (InterceptUserInterfaceIntent) -> Unit,
) {
    val failureReasonResource = uiState.failureReasonResource
    when {
        failureReasonResource != null ->
            item(key = ERROR_KEY) {
                BurpRemoteErrorState(
                    detail = stringResource(failureReasonResource),
                    onRetry = { onIntent(InterceptUserInterfaceIntent.Refresh) },
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

/**
 * 拦截项的一行：左方法色标、中目标（等宽）、右状态胶囊，往下两行给创建与最近变更时刻。
 *
 * 与实时历史的行逐项对齐，因此同一个请求在两屏之间可以直接对照。
 */
@Composable
private fun InterceptRecordCard(
    record: InterceptRecord,
    now: Instant,
    onOpen: () -> Unit,
) {
    val absentValue = stringResource(R.string.intercept_absent_value)

    BurpRemoteCard(isInteractive = true, onClick = onOpen) {
        RequestHeadlineRow(
            method = record.method,
            target = targetTextOf(record = record, absentValue = absentValue),
            absentValue = absentValue,
        )
        BurpRemoteStatusPill(
            text = stringResource(record.state.labelResource),
            tone = toneOf(state = record.state),
        )
        BurpRemoteTechnicalValue(
            text = RECORD_TIME_FORMATTER.format(record.createdAt),
            label = relativeTimeText(now = now, moment = record.createdAt),
        )
        BurpRemoteTechnicalValue(
            text = RECORD_TIME_FORMATTER.format(record.updatedAt),
            label = stringResource(R.string.intercept_detail_updated_at_label),
        )
    }
}

/**
 * 列表共用的头两行：方法色标 + 方法名 + 目标（等宽），状态胶囊紧随其后。
 *
 * 色标与方法名同色，读类、写类、删除三种动词语义各成一组，与实时历史、归档完全一致。
 */
@Composable
private fun RequestHeadlineRow(
    method: String?,
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
    }
}

/** 方法色标：一竖条按方法语义着色，与实时历史、归档是同一个意思。 */
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

@Preview(name = "读取中浅色", showBackground = true)
@Composable
private fun InterceptScreenLoadingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        InterceptScreen(uiState = InterceptUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "读取中深色", showBackground = true)
@Composable
private fun InterceptScreenLoadingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        InterceptScreen(uiState = InterceptUserInterfaceState(), onIntent = {})
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
