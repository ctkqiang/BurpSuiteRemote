package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
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

/**
 * 重放（plan §43 的 Live/Repeater）。
 *
 * 两个「还做不到」的原因必须分开写：一是插件侧还没有对应端点，二是客户端还没有执行命令的通路。
 * 因此新建与执行两个按钮是禁用的，旁边写清缺哪一样，界面上找不到一个点了没反应的地方。
 */
@Composable
fun RepeaterScreen(
    uiState: RepeaterUserInterfaceState,
    onIntent: (RepeaterUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()
    val refresh = {
        haptics.tap()
        onIntent(RepeaterUserInterfaceIntent.Refresh)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        run {
            BurpRemotePullToRefresh(isRefreshing = uiState.isRefreshing, onRefresh = refresh) {
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
                    ConnectionSection(uiState = uiState, onRefresh = refresh)
                    RequestListSection()
                    ExecutionSection()
                }
            }
        }
    }
}

@Composable
private fun ConnectionSection(
    uiState: RepeaterUserInterfaceState,
    onRefresh: () -> Unit,
) {
    val failureReasonResource = uiState.failureReasonResource
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.repeater_connection_heading))
        BurpRemoteCard {
            BurpRemoteStatusPill(
                text = stringResource(modeLabelResource(isLive = uiState.isLive)),
                tone =
                    if (uiState.isLive) {
                        BurpRemoteStatusTone.Live
                    } else {
                        BurpRemoteStatusTone.Warning
                    },
            )
            BurpRemoteTechnicalValue(
                text = stringResource(connectionStateResourceOf(uiState.connectionState)),
                label = stringResource(R.string.repeater_connection_state_label),
            )
        }
        when {
            failureReasonResource != null ->
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.repeater_error_headline),
                    detail = stringResource(failureReasonResource),
                    actionText = stringResource(R.string.repeater_error_action),
                    onAction = onRefresh,
                )

            !uiState.hasLoaded ->
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.repeater_loading_headline),
                    detail = stringResource(R.string.repeater_loading_detail),
                )

            else -> Unit
        }
    }
}

@Composable
private fun RequestListSection() {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.repeater_request_list_heading))
        BurpRemoteEmptyState(
            headline = stringResource(R.string.repeater_request_list_empty_headline),
            detail = stringResource(R.string.repeater_request_list_empty_detail),
        )
        BurpRemoteEmptyState(
            headline = stringResource(R.string.repeater_reason_headline),
            detail = stringResource(R.string.repeater_reason_request_list),
        )
        BurpRemoteButton(
            text = stringResource(R.string.repeater_action_create),
            onClick = {},
            style = BurpRemoteButtonStyle.Secondary,
            isEnabled = false,
        )
    }
}

@Composable
private fun ExecutionSection() {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.repeater_execution_heading))
        BurpRemoteEmptyState(
            headline = stringResource(R.string.repeater_execution_empty_headline),
            detail = stringResource(R.string.repeater_execution_empty_detail),
        )
        BurpRemoteEmptyState(
            headline = stringResource(R.string.repeater_reason_headline),
            detail = stringResource(R.string.repeater_reason_execute),
        )
        BurpRemoteButton(
            text = stringResource(R.string.repeater_action_execute),
            onClick = {},
            style = BurpRemoteButtonStyle.Primary,
            isEnabled = false,
        )
    }
}

private fun modeLabelResource(isLive: Boolean): Int =
    if (isLive) R.string.repeater_mode_live else R.string.repeater_mode_offline

private fun connectionStateResourceOf(connectionState: ConnectionState): Int =
    when (connectionState) {
        ConnectionState.Disconnected -> R.string.repeater_connection_state_disconnected
        ConnectionState.Discovering -> R.string.repeater_connection_state_discovering
        ConnectionState.Connecting -> R.string.repeater_connection_state_connecting
        ConnectionState.Authenticating -> R.string.repeater_connection_state_authenticating
        ConnectionState.Synchronising -> R.string.repeater_connection_state_synchronising
        ConnectionState.Connected -> R.string.repeater_connection_state_connected
        ConnectionState.Reconnecting -> R.string.repeater_connection_state_reconnecting
        ConnectionState.ResynchronisationRequired -> R.string.repeater_connection_state_resynchronisation_required
        ConnectionState.AuthenticationFailed -> R.string.repeater_connection_state_authentication_failed
        ConnectionState.ProtocolError -> R.string.repeater_connection_state_protocol_error
        ConnectionState.TimedOut -> R.string.repeater_connection_state_timed_out
        ConnectionState.ServerUnavailable -> R.string.repeater_connection_state_server_unavailable
    }

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "在线浅色", showBackground = true)
@Composable
private fun RepeaterScreenLiveLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        RepeaterScreen(
            uiState = RepeaterUserInterfaceState(connectionState = ConnectionState.Connected, hasLoaded = true),
            onIntent = {},
        )
    }
}

@Preview(name = "在线深色", showBackground = true)
@Composable
private fun RepeaterScreenLiveDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        RepeaterScreen(
            uiState = RepeaterUserInterfaceState(connectionState = ConnectionState.Connected, hasLoaded = true),
            onIntent = {},
        )
    }
}

@Preview(name = "离线浅色", showBackground = true)
@Composable
private fun RepeaterScreenOfflineLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        RepeaterScreen(uiState = RepeaterUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}

@Preview(name = "离线深色", showBackground = true)
@Composable
private fun RepeaterScreenOfflineDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        RepeaterScreen(uiState = RepeaterUserInterfaceState(hasLoaded = true), onIntent = {})
    }
}
