package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteHistoryMessage
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButtonStyle
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSkeletonRow
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical.BurpRemoteCodeBlock
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical.BurpRemoteCodeLanguage
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.localisation.localisedDateTimeFormatter
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 历史详情（plan §20、plan §22）。
 *
 * 元数据与报文本体分两段画：元数据来自本机投影，立刻就有；本体是向插件按标识取回的一次读取，
 * 因此它有自己的三种状态——还在取、取到了、取失败了。取失败时把失败归类的原文摆出来并给一次重试，
 * 不给重试的那几类（插件不支持、记录已不在）说明白为什么，而不是留一个按了没用处的按钮。
 *
 * 本体里四个文本项各自可为空：插件确实可能没捕获到响应。那种情况下摆一行「没有捕获到这一段」，
 * 不填占位内容冒充正文（rules.md §5.1）。所有技术值等宽显示，长按可整条复制，
 * 排版上不为了好看截断任何一项——赏金猎人的证据链不能被省略号吃掉。
 *
 * 动作区里每一条控制都自带它的处境：能做的按钮就能按；做不了的按钮停用，并在下面一行写明差在
 * 哪里。加入作用域那条更细一层——按下之后的结论（已加入、没配对、连不上……）就地摆一句，
 * 因为这些结论的下一步动作各不相同，统一成一句「失败了」等于把排查全丢回给用户。
 *
 * 标题与返回都归装配层的壳；这一屏只负责内容。
 */
@Composable
fun HistoryDetailScreen(
    uiState: HistoryDetailUserInterfaceState,
    onIntent: (HistoryDetailUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = BurpRemoteSpacing.Large,
                        vertical = BurpRemoteSpacing.Large,
                    ),
            verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
        ) {
            val record = uiState.record
            when {
                record != null ->
                    RecordDetail(
                        record = record,
                        message = uiState.message,
                        messageFailure = uiState.messageFailure,
                        scopeConclusion = uiState.scopeConclusion,
                        isScopeWriteAvailable = uiState.isScopeWriteAvailable,
                        repeaterConclusion = uiState.repeaterConclusion,
                        isRepeaterWriteAvailable = uiState.isRepeaterWriteAvailable,
                        onReloadMessage = {
                            haptics.tap()
                            onIntent(HistoryDetailUserInterfaceIntent.ReloadHistoryMessage)
                        },
                        onAddToScope = {
                            onIntent(HistoryDetailUserInterfaceIntent.AddHistoryHostToScope)
                        },
                        onSendToRepeater = {
                            haptics.tap()
                            onIntent(HistoryDetailUserInterfaceIntent.SendToRepeater)
                        },
                        onShare = {
                            haptics.tap()
                            onIntent(HistoryDetailUserInterfaceIntent.ShareHistoryRecord)
                        },
                    )

                uiState.hasLoaded ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.history_detail_not_found_headline),
                        detail = stringResource(R.string.history_detail_not_found),
                    )

                else ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.history_detail_loading_headline),
                        detail = stringResource(R.string.history_detail_loading),
                    )
            }
        }
    }
}

@Composable
private fun RecordDetail(
    record: HistoryRecord,
    message: RemoteHistoryMessage?,
    messageFailure: HistoryMessageReadFailure?,
    scopeConclusion: HistoryScopeWriteConclusion?,
    isScopeWriteAvailable: Boolean,
    repeaterConclusion: HistoryRepeaterConclusion?,
    isRepeaterWriteAvailable: Boolean,
    onReloadMessage: () -> Unit,
    onAddToScope: () -> Unit,
    onSendToRepeater: () -> Unit,
    onShare: () -> Unit,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    val absentValue = stringResource(R.string.history_absent_value)

    // 第一行先给判读结果：方法与状态码，往下才是逐项明细。
    Section(titleResource = R.string.history_detail_summary_heading) {
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
        BurpRemoteTechnicalValue(
            text = "${record.method ?: absentValue} ${record.path ?: absentValue}",
            label = stringResource(R.string.history_detail_request_line_label),
        )
        BurpRemoteTechnicalValue(
            text = record.host ?: absentValue,
            label = stringResource(R.string.history_detail_host_label),
        )
    }

    Section(titleResource = R.string.history_detail_request_heading) {
        BurpRemoteTechnicalValue(
            text = record.scheme ?: absentValue,
            label = stringResource(R.string.history_detail_scheme_label),
        )
        BurpRemoteTechnicalValue(
            text = booleanTextOf(value = record.usesTls, absentValue = absentValue),
            label = stringResource(R.string.history_detail_tls_label),
        )
        BurpRemoteTechnicalValue(
            text = record.destinationInternetProtocolAddress ?: absentValue,
            label = stringResource(R.string.history_detail_destination_address_label),
        )
        BurpRemoteTechnicalValue(
            text = record.listenerPort?.toString() ?: absentValue,
            label = stringResource(R.string.history_detail_listener_port_label),
        )
    }

    Section(titleResource = R.string.history_detail_response_heading) {
        BurpRemoteTechnicalValue(
            text = record.statusCode?.toString() ?: absentValue,
            label = stringResource(R.string.history_detail_status_code_label),
        )
        BurpRemoteTechnicalValue(
            text = record.mimeType ?: absentValue,
            label = stringResource(R.string.history_detail_mime_type_label),
        )
        BurpRemoteTechnicalValue(
            text = responseLengthTextOf(record = record, absentValue = absentValue),
            label = stringResource(R.string.history_detail_response_length_label),
        )
        BurpRemoteTechnicalValue(
            text = durationTextOf(record = record, absentValue = absentValue),
            label = stringResource(R.string.history_detail_duration_label),
        )
    }

    Section(titleResource = R.string.history_detail_record_heading) {
        BurpRemoteTechnicalValue(
            text = record.savedAt?.let { savedAt -> RECORD_TIME_FORMATTER.format(savedAt) } ?: absentValue,
            label = stringResource(R.string.history_detail_saved_at_label),
        )
        BurpRemoteTechnicalValue(
            text = record.annotationCount.toString(),
            label = stringResource(R.string.history_detail_annotation_count_label),
        )
        BurpRemoteTechnicalValue(
            text =
                record.lastAnnotatedAt?.let { lastAnnotatedAt -> RECORD_TIME_FORMATTER.format(lastAnnotatedAt) }
                    ?: absentValue,
            label = stringResource(R.string.history_detail_last_annotated_at_label),
        )
        BurpRemoteTechnicalValue(
            text = record.title ?: absentValue,
            label = stringResource(R.string.history_detail_title_label),
        )
        BurpRemoteTechnicalValue(
            text = booleanTextOf(value = record.isEdited, absentValue = absentValue),
            label = stringResource(R.string.history_detail_edited_label),
        )
    }

    Section(titleResource = R.string.history_detail_timeline_heading) {
        BurpRemoteTechnicalValue(
            text = record.sequenceNumber?.toString() ?: absentValue,
            label = stringResource(R.string.history_detail_sequence_number_label),
        )
        BurpRemoteTechnicalValue(
            text = RECORD_TIME_FORMATTER.format(record.occurredAt),
            label = stringResource(R.string.history_detail_occurred_at_label),
        )
    }

    MessageSection(
        message = message,
        messageFailure = messageFailure,
        onReloadMessage = onReloadMessage,
    )

    Section(titleResource = R.string.history_detail_actions_heading) {
        Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
            BurpRemoteButton(
                text = stringResource(R.string.history_detail_action_share),
                onClick = onShare,
                style = BurpRemoteButtonStyle.Primary,
            )
            // 加入作用域加到这条记录的主机，整台主机下的所有路径一并覆盖；按键自带触感，这里不再叠一次。
            BurpRemoteButton(
                text = stringResource(R.string.history_detail_action_add_to_scope),
                onClick = onAddToScope,
                style = BurpRemoteButtonStyle.Secondary,
                isEnabled = isScopeWriteAvailable,
            )
            // 停用的按钮要说明差在哪里：这是装配缺口（客户端没接上写入端口），不是插件版本的问题。
            if (!isScopeWriteAvailable) {
                BurpRemoteText(
                    text = stringResource(R.string.history_detail_reason_add_to_scope),
                    style = tokens.typography.label,
                    colour = tokens.colourScheme.contentSecondary,
                )
            }
            // 按下的结论就地摆一句并按类着色——每种结论的下一步都不同，合并成一句「失败了」等于把排查丢回给用户。
            scopeConclusion?.let { conclusion ->
                BurpRemoteStatusPill(
                    text = stringResource(conclusion.messageResource),
                    tone = toneOf(conclusion),
                )
            }
            // 送往重放把这条请求推到 Repeater；写入端口没接上或本体还没取回时按钮停用并说明原因。
            BurpRemoteButton(
                text = stringResource(R.string.history_detail_action_send_to_repeater),
                onClick = onSendToRepeater,
                style = BurpRemoteButtonStyle.Secondary,
                isEnabled = isRepeaterWriteAvailable,
            )
            if (!isRepeaterWriteAvailable) {
                BurpRemoteText(
                    text = stringResource(R.string.history_detail_reason_send_to_repeater),
                    style = tokens.typography.label,
                    colour = tokens.colourScheme.contentSecondary,
                )
            }
            // 按下的结论就地摆一句并按类着色——每种结论的下一步都不同，合并成一句「失败了」等于把排查丢回给用户。
            repeaterConclusion?.let { conclusion ->
                BurpRemoteStatusPill(
                    text = stringResource(conclusion.messageResource),
                    tone = toneOf(conclusion),
                )
            }
        }
    }
}

/**
 * 报文本体那一节。
 *
 * 这一节不套卡片：四段正文各自就是一个代码块，代码块自带表面色与描边，再套一层卡片只会
 * 出现两层边框套在一起。所以标题照旧，内容直接落在页面上。
 */
@Composable
private fun MessageSection(
    message: RemoteHistoryMessage?,
    messageFailure: HistoryMessageReadFailure?,
    onReloadMessage: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.history_detail_message_heading))
        when {
            message != null -> MessageBodies(message = message)

            messageFailure != null ->
                BurpRemoteCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
                        BurpRemoteStatusPill(
                            text = stringResource(messageFailure.messageResource),
                            tone = toneOf(messageFailure),
                        )
                        if (messageFailure.isRetryable) {
                            BurpRemoteButton(
                                text = stringResource(R.string.history_detail_message_retry),
                                onClick = onReloadMessage,
                                style = BurpRemoteButtonStyle.Secondary,
                            )
                        }
                    }
                }

            // 还没读到结果：骨架行先说清「马上要出现什么」，而不是把空白留在那里。
            else ->
                BurpRemoteCard {
                    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
                        BurpRemoteSkeletonRow()
                        BurpRemoteSkeletonRow()
                    }
                }
        }
    }
}

@Composable
private fun MessageBodies(message: RemoteHistoryMessage) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium)) {
        BodyBlock(
            labelResource = R.string.history_detail_request_headers_label,
            text = message.requestHeaders,
        )
        BodyBlock(
            labelResource = R.string.history_detail_request_body_label,
            text = message.requestBody,
        )
        BodyBlock(
            labelResource = R.string.history_detail_response_headers_label,
            text = message.responseHeaders,
        )
        BodyBlock(
            labelResource = R.string.history_detail_response_body_label,
            text = message.responseBody,
        )
    }
}

/**
 * 一段正文：标签加代码块。
 *
 * [text] 为空表示插件没有捕获到这一段，此时摆一行说明而留空——空代码块看起来像「这段是空的」，
 * 那是另一件事。
 */
@Composable
private fun BodyBlock(
    @StringRes labelResource: Int,
    text: String?,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraSmall)) {
        BurpRemoteText(
            text = stringResource(labelResource),
            style = tokens.typography.label,
            colour = tokens.colourScheme.contentSecondary,
        )
        if (text.isNullOrEmpty()) {
            BurpRemoteText(
                text = stringResource(R.string.history_detail_message_absent),
                style = tokens.typography.technical,
                colour = tokens.colourScheme.contentSecondary,
            )
        } else {
            BurpRemoteCodeBlock(
                text = text,
                language = codeLanguageOf(text),
            )
        }
    }
}

/** 一节标题加一块内容；各节的排版一致，免得同一层级的字段在不同节里长得不一样。 */
@Composable
private fun Section(
    @StringRes titleResource: Int,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(titleResource))
        BurpRemoteCard { content() }
    }
}

@Composable
private fun booleanTextOf(
    value: Boolean?,
    absentValue: String,
): String =
    when (value) {
        true -> stringResource(R.string.history_detail_value_yes)
        false -> stringResource(R.string.history_detail_value_no)
        null -> absentValue
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

/** 方法色标按动词语义分档，与三处列表同一套：读类中性偏在线，写类警告，删除危险。 */
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

/** 失败归类的色标：分不清是谁的错就危险；确定没法重试的中性；其余警告。 */
private fun toneOf(failure: HistoryMessageReadFailure): BurpRemoteStatusTone =
    when (failure) {
        HistoryMessageReadFailure.MalformedResponse -> BurpRemoteStatusTone.Danger
        HistoryMessageReadFailure.NotSupported,
        HistoryMessageReadFailure.RecordMissing,
        -> BurpRemoteStatusTone.Neutral

        HistoryMessageReadFailure.NotPaired,
        HistoryMessageReadFailure.Unreachable,
        HistoryMessageReadFailure.Refused,
        -> BurpRemoteStatusTone.Warning
    }

/**
 * 加入作用域的结论色标：写进去了算在线；学不会契约内容的应答危险；确定重试无用的中性；
 * 其余（没配对、连不上、被拒）警告。
 *
 * 与上面那条失败色标同一套分档逻辑，只是成员不同——同一种颜色在两条通路里表示同一件事，
 * 用户不用重新学一套图例。
 */
private fun toneOf(conclusion: HistoryScopeWriteConclusion): BurpRemoteStatusTone =
    when (conclusion) {
        HistoryScopeWriteConclusion.Added -> BurpRemoteStatusTone.Live
        HistoryScopeWriteConclusion.MalformedResponse -> BurpRemoteStatusTone.Danger
        HistoryScopeWriteConclusion.NotSupported,
        HistoryScopeWriteConclusion.WriterUnavailable,
        HistoryScopeWriteConclusion.RecordMissing,
        -> BurpRemoteStatusTone.Neutral

        HistoryScopeWriteConclusion.NotPaired,
        HistoryScopeWriteConclusion.Unreachable,
        HistoryScopeWriteConclusion.Refused,
        -> BurpRemoteStatusTone.Warning
    }

/**
 * 送给重放的结论色标：与作用域写入同一套分档——成功在线、学不会契约的危险、重试无用的中性、其余警告。
 */
private fun toneOf(conclusion: HistoryRepeaterConclusion): BurpRemoteStatusTone =
    when (conclusion) {
        HistoryRepeaterConclusion.Sent -> BurpRemoteStatusTone.Live
        HistoryRepeaterConclusion.MalformedResponse -> BurpRemoteStatusTone.Danger
        HistoryRepeaterConclusion.NotSupported,
        HistoryRepeaterConclusion.WriterUnavailable,
        HistoryRepeaterConclusion.MessageNotLoaded,
        -> BurpRemoteStatusTone.Neutral

        HistoryRepeaterConclusion.NotPaired,
        HistoryRepeaterConclusion.Unreachable,
        HistoryRepeaterConclusion.Refused,
        -> BurpRemoteStatusTone.Warning
    }

/**
 * 挑语法着色：正文以 `{` 或 `[` 开头就按 JSON 上色。
 *
 * 请求头与响应头是 HTTP 文本，走纯文本；这一条判断只为了让响应体的 JSON 有字段名、字符串、
 * 数字的分色——赏金猎人扫一眼响应体，分色比一整片同色字快得多。
 */
private fun codeLanguageOf(text: String): BurpRemoteCodeLanguage =
    when (text.trimStart().firstOrNull()) {
        JSON_OBJECT_OPEN, JSON_ARRAY_OPEN -> BurpRemoteCodeLanguage.Json
        else -> BurpRemoteCodeLanguage.PlainText
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

private const val JSON_OBJECT_OPEN = '{'
private const val JSON_ARRAY_OPEN = '['

// 时刻按设备时区与当前语言格式化；同一秒内的多条记录靠秒级时间区分，所以这里带日期。
private val RECORD_TIME_FORMATTER: DateTimeFormatter
    get() = localisedDateTimeFormatter(FormatStyle.SHORT)

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun HistoryDetailScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryDetailScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun HistoryDetailScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryDetailScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "未找到浅色", showBackground = true)
@Composable
private fun HistoryDetailScreenMissingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryDetailScreen(uiState = HistoryDetailUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "未找到深色", showBackground = true)
@Composable
private fun HistoryDetailScreenMissingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryDetailScreen(uiState = HistoryDetailUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "读取中浅色", showBackground = true)
@Composable
private fun HistoryDetailScreenLoadingMessageLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryDetailScreen(
            uiState = previewState().copy(message = null),
            onIntent = {},
        )
    }
}

@Preview(name = "读取失败深色", showBackground = true)
@Composable
private fun HistoryDetailScreenFailedMessageDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryDetailScreen(
            uiState =
                previewState().copy(
                    message = null,
                    messageFailure = HistoryMessageReadFailure.Unreachable,
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "已加入作用域浅色", showBackground = true)
@Composable
private fun HistoryDetailScreenScopeAddedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryDetailScreen(
            uiState = previewState().copy(scopeConclusion = HistoryScopeWriteConclusion.Added),
            onIntent = {},
        )
    }
}

@Preview(name = "无作用域通路深色", showBackground = true)
@Composable
private fun HistoryDetailScreenScopeUnavailableDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryDetailScreen(
            uiState = previewState().copy(isScopeWriteAvailable = false),
            onIntent = {},
        )
    }
}

@Preview(name = "已送往重放浅色", showBackground = true)
@Composable
private fun HistoryDetailScreenRepeaterSentLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        HistoryDetailScreen(
            uiState = previewState().copy(repeaterConclusion = HistoryRepeaterConclusion.Sent),
            onIntent = {},
        )
    }
}

@Preview(name = "无重放通路深色", showBackground = true)
@Composable
private fun HistoryDetailScreenRepeaterUnavailableDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        HistoryDetailScreen(
            uiState = previewState().copy(isRepeaterWriteAvailable = false),
            onIntent = {},
        )
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewState(): HistoryDetailUserInterfaceState =
    HistoryDetailUserInterfaceState(
        record =
            HistoryRecord(
                historyIdentifier = HistoryIdentifier(value = "history_123"),
                sequenceNumber = 5001L,
                occurredAt = Instant.parse("2026-09-13T08:00:00Z"),
                host = "api.example.com",
                method = "GET",
                scheme = "https",
                path = "/api/user",
                statusCode = 200,
                mimeType = "application/json",
                responseLength = 1024L,
                usesTls = true,
                destinationInternetProtocolAddress = "203.0.113.10",
                listenerPort = 8080,
                durationMilliseconds = 42L,
                isEdited = false,
                title = null,
                archiveState = HistoryArchiveState.Archived,
                annotationCount = 2,
                lastAnnotatedAt = Instant.parse("2026-09-13T08:05:00Z"),
                savedAt = Instant.parse("2026-09-13T08:01:00Z"),
            ),
        hasLoaded = true,
        message =
            RemoteHistoryMessage(
                historyIdentifier = "history_123",
                method = "GET",
                host = "api.example.com",
                path = "/api/user",
                statusCode = 200,
                requestHeaders =
                    "GET /api/user HTTP/1.1\r\nHost: api.example.com\r\n" +
                        "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.example\r\n" +
                        "Accept: application/json\r\n",
                requestBody = null,
                responseHeaders =
                    "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n" +
                        "Content-Length: 1024\r\n",
                responseBody = """{"identifier":"user_1","displayName":"小哪吒","roles":["admin"]}""",
            ),
    )
