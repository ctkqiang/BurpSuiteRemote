// Repeater 请求列表屏。

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.RepeaterRequestIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.RepeaterRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemotePullToRefresh
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens
import java.time.Instant

/**
 * 重放屏（plan §43 的 Live/Repeater）。
 *
 * 读端口从 RepeaterRepository 来（事件驱动投影），列表按创建时刻降序。
 * 执行命令走独立 REST 端点，不从 ViewModel 绕回去（rules.md §5.1）。
 */
@Composable
fun RepeaterScreen(
    uiState: RepeaterUserInterfaceState,
    onIntent: (RepeaterUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refresh = { onIntent(RepeaterUserInterfaceIntent.Refresh) }

    Box(modifier = modifier.fillMaxSize()) {
        BurpRemotePullToRefresh(isRefreshing = uiState.isRefreshing, onRefresh = refresh) {
            when {
                uiState.failureReasonResource != null ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.repeater_error_headline),
                        detail = stringResource(uiState.failureReasonResource),
                        actionText = stringResource(R.string.repeater_error_action),
                        onAction = refresh,
                    )

                !uiState.hasLoaded ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.repeater_loading_headline),
                        detail = stringResource(R.string.repeater_loading_detail),
                    )

                uiState.records.isEmpty() ->
                    BurpRemoteEmptyState(
                        headline = stringResource(R.string.repeater_request_list_empty_headline),
                        detail = stringResource(R.string.repeater_request_list_empty_detail),
                    )

                else ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Medium),
                        contentPadding =
                            PaddingValues(
                                horizontal = BurpRemoteSpacing.Large,
                                vertical = BurpRemoteSpacing.Large,
                            ),
                    ) {
                        items(
                            items = uiState.records,
                            key = { record -> record.repeaterRequestIdentifier.value },
                        ) { record ->
                            RepeaterRecordCard(record = record)
                        }
                    }
            }
        }
    }
}

/** 单条 Repeater 请求卡片。 */
@Composable
private fun RepeaterRecordCard(record: RepeaterRecord) {
    val tokens = LocalBurpRemoteDesignTokens.current
    BurpRemoteCard {
        Column(
            modifier = Modifier.padding(BurpRemoteSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small),
        ) {
            RowItem(
                label = stringResource(R.string.repeater_card_tab_label),
                value = record.tabName ?: "-",
            )
            RowItem(
                label = stringResource(R.string.repeater_card_method_label),
                value = extractMethod(record.requestText) ?: "-",
            )
            RowItem(
                label = stringResource(R.string.repeater_card_status_label),
                value = renderStatus(record),
            )
            RowItem(
                label = stringResource(R.string.repeater_card_executing_label),
                value = if (record.isExecuting) "..." else "-",
            )
        }
    }
}

@Composable
private fun RowItem(
    label: String,
    value: String,
) {
    val tokens = LocalBurpRemoteDesignTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(text = label, style = tokens.typography.technical)
        BasicText(text = value, style = tokens.typography.technical)
    }
}

/** 从原始请求文本里提取 HTTP 方法；空安全。 */
private fun extractMethod(requestText: String?): String? =
    requestText
        ?.lineSequence()
        ?.firstOrNull()
        ?.substringBefore(' ')
        ?.takeIf { it.isNotBlank() }

/** 友好地呈现最后执行结果。 */
@Composable
private fun renderStatus(record: RepeaterRecord): String =
    when {
        record.lastExecutionFailed == true ->
            stringResource(R.string.repeater_card_status_failed)

        record.lastStatusCode != null ->
            record.lastStatusCode.toString()

        else ->
            stringResource(R.string.repeater_card_status_never)
    }

// --- 预览 ---

@Preview(name = "列表有数据 浅色", showBackground = true)
@Composable
private fun RepeaterScreenLoadedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        RepeaterScreen(
            uiState =
                RepeaterUserInterfaceState(
                    connectionState = ConnectionState.Connected,
                    hasLoaded = true,
                    records =
                        listOf(
                            RepeaterRecord(
                                repeaterRequestIdentifier = RepeaterRequestIdentifier("abc-123"),
                                sequenceNumber = 1L,
                                createdAt = Instant.now(),
                                updatedAt = Instant.now(),
                                requestText = "GET /api/users HTTP/1.1\nHost: example.com",
                                tabName = "users-api",
                                lastExecutedAt = Instant.now(),
                                lastStatusCode = 200,
                                lastDurationMilliseconds = 152L,
                                lastExecutionFailed = false,
                                isExecuting = false,
                            ),
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview(name = "空列表 浅色", showBackground = true)
@Composable
private fun RepeaterScreenEmptyLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        RepeaterScreen(
            uiState = RepeaterUserInterfaceState(hasLoaded = true),
            onIntent = {},
        )
    }
}

@Preview(name = "加载中 浅色", showBackground = true)
@Composable
private fun RepeaterScreenLoadingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        RepeaterScreen(uiState = RepeaterUserInterfaceState(), onIntent = {})
    }
}
