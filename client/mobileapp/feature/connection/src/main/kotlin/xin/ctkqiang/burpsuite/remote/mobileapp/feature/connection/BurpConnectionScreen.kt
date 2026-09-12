package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.RemoteServerEndpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButtonStyle
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteCard
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTextField
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingConclusion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Burp 连接（plan §43 里设置区的第一项，plan §47 的地址端口，plan §55 的配对流程）。
 *
 * 无状态：状态由外面传进来，用户意图往外抛。扫码面板打开时整屏让给它——取景框太小二维码对不上焦。
 *
 * 说明性段落一律用 [BurpRemoteEmptyState] 承载：设计系统里它是唯一能容纳整句文案的组件，而且
 * 「缺什么、下一步做什么」正是它要表达的东西。
 */
@Composable
fun BurpConnectionScreen(
    uiState: BurpConnectionUserInterfaceState,
    onIntent: (BurpConnectionUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberBurpRemoteHaptics()

    // 扫码是弹窗不是页面：装配层用同一个全屏 Dialog 承载它，退出后落回本屏内容。
    // 冻结契约只认两个回调，因此这里不把 ViewModel 的状态灌进去——识别、校验、权限都在弹窗内闭环。
    if (uiState.isScannerOpen) {
        Dialog(
            onDismissRequest = { onIntent(BurpConnectionUserInterfaceIntent.ClosePairingScanner) },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            BurpRemoteScannerDialog(
                onDismiss = { onIntent(BurpConnectionUserInterfaceIntent.ClosePairingScanner) },
                onScannedTicketText = { encodedTicketText ->
                    onIntent(BurpConnectionUserInterfaceIntent.PairTicketText(encodedTicketText))
                },
            )
        }
        return
    }

    // 配对出结果、权限被拒都是「刚刚发生了一件事」，各自给一记不同强度的触感。
    LaunchedEffect(uiState.pairingOutcome) {
        when (uiState.pairingOutcome) {
            PairingOutcome.Succeeded -> haptics.success()
            PairingOutcome.Rejected -> haptics.failure()
            null -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题。
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SCREEN_PADDING, vertical = SCREEN_PADDING),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            ConnectionStateSection(uiState = uiState)
            ManualPairingSection(
                uiState = uiState,
                onIntent = onIntent,
                onAction = { haptics.tap() },
            )
            PairingTicketSection(uiState = uiState)
            PairingOutcomeSection(uiState = uiState)
            PairedDeviceSection(
                uiState = uiState,
                onIntent = onIntent,
                onAction = { haptics.tap() },
            )
            EndpointSection(
                uiState = uiState,
                onIntent = onIntent,
                onAction = { haptics.tap() },
            )
            RetrySection(
                uiState = uiState,
                onRetry = {
                    haptics.tap()
                    onIntent(BurpConnectionUserInterfaceIntent.RetryConnection)
                },
            )
        }
    }
}

@Composable
private fun ConnectionStateSection(uiState: BurpConnectionUserInterfaceState) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_status_heading))
        BurpRemoteCard {
            BurpRemoteStatusPill(
                text = stringResource(uiState.connectionState.labelResource),
                tone = connectionStateTone(connectionState = uiState.connectionState),
            )
            BurpRemoteStatusPill(
                text = stringResource(modeLabelResource(isLive = uiState.isLive)),
                tone = if (uiState.isLive) BurpRemoteStatusTone.Live else BurpRemoteStatusTone.Neutral,
            )
            BurpRemoteTechnicalValue(
                text = uiState.savedEndpoint?.host ?: stringResource(R.string.connection_server_address_absent),
                label = stringResource(R.string.connection_server_address_label),
            )
            BurpRemoteTechnicalValue(
                text = uiState.savedEndpoint?.port?.toString() ?: stringResource(R.string.connection_absent_value),
                label = stringResource(R.string.connection_server_port_label),
            )
        }
    }
}

@Composable
private fun ManualPairingSection(
    uiState: BurpConnectionUserInterfaceState,
    onIntent: (BurpConnectionUserInterfaceIntent) -> Unit,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_manual_pairing_heading))
        BurpRemoteEmptyState(
            headline = stringResource(R.string.connection_manual_pairing_headline),
            detail = stringResource(R.string.connection_manual_pairing_hint),
        )
        BurpRemoteTextField(
            value = uiState.pairingTextInput,
            onValueChange = { pairingTextInput ->
                onIntent(BurpConnectionUserInterfaceIntent.UpdatePairingTextInput(pairingTextInput))
            },
            label = stringResource(R.string.connection_manual_pairing_text_label),
            // 配对文本要能逐字符比对，比例字体做不到这件事。
            isMonospace = true,
        )
        BurpRemoteButton(
            text = stringResource(R.string.connection_manual_pairing_action),
            onClick = {
                onAction()
                onIntent(BurpConnectionUserInterfaceIntent.StartPairing)
            },
            style = BurpRemoteButtonStyle.Primary,
            isEnabled = uiState.canStartPairing && uiState.unavailableReasonResource == null,
        )
        uiState.unavailableReasonResource?.let { reasonResource ->
            BurpRemoteEmptyState(
                headline = stringResource(R.string.connection_unavailable_headline),
                detail = stringResource(reasonResource),
            )
        }
    }
}

@Composable
private fun PairingTicketSection(uiState: BurpConnectionUserInterfaceState) {
    val ticket = uiState.scannedTicket
    val rejection = uiState.ticketRejection
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_ticket_heading))
        if (ticket != null) {
            BurpRemoteCard {
                BurpRemoteTechnicalValue(
                    text = ticket.host,
                    label = stringResource(R.string.connection_ticket_host_label),
                )
                BurpRemoteTechnicalValue(
                    text = ticket.port.toString(),
                    label = stringResource(R.string.connection_ticket_port_label),
                )
                BurpRemoteTechnicalValue(
                    text = ticket.protocolVersion.toString(),
                    label = stringResource(R.string.connection_ticket_protocol_version_label),
                )
                BurpRemoteTechnicalValue(
                    text = TICKET_EXPIRY_FORMATTER.format(ticket.expiresAt),
                    label = stringResource(R.string.connection_ticket_expires_at_label),
                )
            }
        }
        if (rejection == null) {
            BurpRemoteEmptyState(
                headline = stringResource(R.string.connection_ticket_absent_headline),
                detail = stringResource(R.string.connection_ticket_absent_detail),
            )
        } else {
            BurpRemoteEmptyState(
                headline = stringResource(ticketRejectionHeadlineResource(rejection)),
                detail = ticketRejectionDetail(uiState = uiState, rejection = rejection),
            )
        }
    }
}

/**
 * 配对结果。
 *
 * 标题是这次尝试的结论本身——每一种结论都对应不同的下一步；下面再把这份结论的归类写清楚，
 * 于是既没有「统一成一句失败」，也没有两处重复同一句话。
 */
@Composable
private fun PairingOutcomeSection(uiState: BurpConnectionUserInterfaceState) {
    val pairingOutcome = uiState.pairingOutcome
    val conclusion = uiState.pairingConclusion
    if (pairingOutcome == null || conclusion == null) return

    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_pairing_result_heading))
        when (pairingOutcome) {
            PairingOutcome.Succeeded ->
                BurpRemoteEmptyState(
                    headline = stringResource(R.string.connection_pairing_succeeded_headline),
                    detail = stringResource(R.string.connection_pairing_succeeded_detail),
                )

            PairingOutcome.Rejected ->
                BurpRemoteEmptyState(
                    headline = stringResource(conclusion.messageResource),
                    detail = stringResource(pairingRejectionResourceOf(uiState.pairingRejectionReason)),
                )
        }
    }
}

/** 已配对时这里显示的是票据给的那份值；两个入口分别对应「再扫一次」与「彻底忘掉本机」。 */
@Composable
private fun PairedDeviceSection(
    uiState: BurpConnectionUserInterfaceState,
    onIntent: (BurpConnectionUserInterfaceIntent) -> Unit,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_paired_heading))
        BurpRemoteCard {
            BurpRemoteStatusPill(
                text =
                    stringResource(
                        if (uiState.isPaired) {
                            R.string.connection_paired_state_paired
                        } else {
                            R.string.connection_paired_state_unpaired
                        },
                    ),
                tone = if (uiState.isPaired) BurpRemoteStatusTone.Live else BurpRemoteStatusTone.Neutral,
            )
            uiState.savedEndpoint?.let { endpoint ->
                BurpRemoteTechnicalValue(
                    text = endpoint.host,
                    label = stringResource(R.string.connection_paired_host_label),
                )
                BurpRemoteTechnicalValue(
                    text = endpoint.port.toString(),
                    label = stringResource(R.string.connection_paired_port_label),
                )
            }
        }
        BurpRemoteEmptyState(
            headline = stringResource(R.string.connection_paired_headline),
            detail =
                stringResource(
                    if (uiState.isPaired) {
                        R.string.connection_paired_detail_paired
                    } else {
                        R.string.connection_paired_detail_unpaired
                    },
                ),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
            Box(modifier = Modifier.weight(1f)) {
                BurpRemoteButton(
                    text = stringResource(R.string.connection_paired_re_pair_action),
                    onClick = {
                        onAction()
                        onIntent(BurpConnectionUserInterfaceIntent.OpenPairingScanner)
                    },
                    style = BurpRemoteButtonStyle.Secondary,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                BurpRemoteButton(
                    text = stringResource(R.string.connection_paired_clear_action),
                    onClick = {
                        onAction()
                        onIntent(BurpConnectionUserInterfaceIntent.ClearPairing)
                    },
                    style = BurpRemoteButtonStyle.Danger,
                    isEnabled = uiState.isPaired || uiState.savedEndpoint != null,
                )
            }
        }
    }
}

/** 地址与端口可编辑并持久化（plan §47）；扫码配对成功后这里显示的就是票据里的那份值。 */
@Composable
private fun EndpointSection(
    uiState: BurpConnectionUserInterfaceState,
    onIntent: (BurpConnectionUserInterfaceIntent) -> Unit,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_endpoint_heading))
        BurpRemoteEmptyState(
            headline = stringResource(R.string.connection_endpoint_headline),
            detail = stringResource(R.string.connection_endpoint_detail, DEFAULT_REMOTE_PORT),
        )
        BurpRemoteTextField(
            value = uiState.hostInput,
            onValueChange = { hostInput ->
                onIntent(BurpConnectionUserInterfaceIntent.UpdateHostInput(hostInput))
            },
            label = stringResource(R.string.connection_endpoint_host_label),
            isMonospace = true,
        )
        BurpRemoteTextField(
            value = uiState.portInput,
            onValueChange = { portInput ->
                onIntent(BurpConnectionUserInterfaceIntent.UpdatePortInput(portInput))
            },
            label = stringResource(R.string.connection_endpoint_port_label),
            isMonospace = true,
        )
        BurpRemoteButton(
            text = stringResource(R.string.connection_endpoint_save_action),
            onClick = {
                onAction()
                onIntent(BurpConnectionUserInterfaceIntent.SaveEndpoint)
            },
            style = BurpRemoteButtonStyle.Secondary,
            isEnabled = uiState.isEndpointSavable,
        )
    }
}

@Composable
private fun RetrySection(
    uiState: BurpConnectionUserInterfaceState,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_retry_heading))
        BurpRemoteEmptyState(
            headline = stringResource(R.string.connection_retry_headline),
            detail = stringResource(R.string.connection_retry_detail),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
            Box(modifier = Modifier.weight(1f)) {
                BurpRemoteButton(
                    text = stringResource(R.string.connection_retry_action),
                    onClick = onRetry,
                    style = BurpRemoteButtonStyle.Secondary,
                    isEnabled = uiState.canRetryConnection && uiState.unavailableReasonResource == null,
                )
            }
            if (uiState.isPairingInFlight) {
                BurpRemoteStatusPill(
                    text = stringResource(R.string.connection_pairing_in_flight),
                    tone = BurpRemoteStatusTone.Neutral,
                )
            }
        }
    }
}

private fun modeLabelResource(isLive: Boolean): Int =
    if (isLive) R.string.connection_mode_live else R.string.connection_mode_offline

private fun connectionStateTone(connectionState: ConnectionState): BurpRemoteStatusTone =
    when {
        connectionState.isConnected -> BurpRemoteStatusTone.Live
        connectionState == ConnectionState.Disconnected -> BurpRemoteStatusTone.Neutral
        connectionState in FAILURE_STATES -> BurpRemoteStatusTone.Danger
        else -> BurpRemoteStatusTone.Warning
    }

/** 协议版本不符、超时这类状态都算故障，配色统一走危险档。 */
private val FAILURE_STATES: Set<ConnectionState> =
    setOf(
        ConnectionState.AuthenticationFailed,
        ConnectionState.ProtocolError,
        ConnectionState.TimedOut,
        ConnectionState.ServerUnavailable,
        ConnectionState.ResynchronisationRequired,
    )

@StringRes
private fun ticketRejectionHeadlineResource(rejection: PairingTicketRejection): Int =
    when (rejection) {
        PairingTicketRejection.Undecodable -> R.string.connection_ticket_rejection_undecodable_headline
        PairingTicketRejection.Expired -> R.string.connection_ticket_rejection_expired_headline
        PairingTicketRejection.ProtocolVersionUnsupported ->
            R.string.connection_ticket_rejection_protocol_headline
    }

@Composable
private fun ticketRejectionDetail(
    uiState: BurpConnectionUserInterfaceState,
    rejection: PairingTicketRejection,
): String {
    val ticket = uiState.scannedTicket
    return when (rejection) {
        PairingTicketRejection.Undecodable -> stringResource(R.string.connection_ticket_rejection_undecodable_detail)
        PairingTicketRejection.Expired ->
            stringResource(
                R.string.connection_ticket_rejection_expired_detail,
                ticket?.expiresAt?.let { expiresAt -> TICKET_EXPIRY_FORMATTER.format(expiresAt) }
                    ?: stringResource(R.string.connection_absent_value),
            )

        PairingTicketRejection.ProtocolVersionUnsupported ->
            stringResource(
                R.string.connection_ticket_rejection_protocol_detail,
                ticket?.protocolVersion ?: uiState.supportedProtocolVersion,
                uiState.supportedProtocolVersion,
            )
    }
}

@StringRes
private fun pairingRejectionResourceOf(reason: PairingRejectionReason?): Int =
    when (reason) {
        PairingRejectionReason.PairingCodeRejected -> R.string.connection_pairing_rejection_code
        PairingRejectionReason.PairingSessionExpired -> R.string.connection_pairing_rejection_session_expired
        PairingRejectionReason.PairingSessionUnavailable -> R.string.connection_pairing_rejection_session_unavailable
        PairingRejectionReason.ProtocolVersionUnsupported -> R.string.connection_pairing_rejection_protocol
        PairingRejectionReason.AuthenticationRejected -> R.string.connection_pairing_rejection_authentication
        PairingRejectionReason.ServerUnreachable -> R.string.connection_pairing_rejection_unreachable
        PairingRejectionReason.ActionNotSupported -> R.string.connection_pairing_rejection_not_supported
        PairingRejectionReason.Unknown, null -> R.string.connection_pairing_rejection_unknown
    }

// 票据失效时刻按设备时区与当前语言格式化：用户要拿它跟插件界面上显示的时间对齐。
private val TICKET_EXPIRY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

@get:StringRes
internal val ConnectionState.labelResource: Int
    get() =
        when (this) {
            ConnectionState.Disconnected -> R.string.connection_state_disconnected
            ConnectionState.Discovering -> R.string.connection_state_discovering
            ConnectionState.Connecting -> R.string.connection_state_connecting
            ConnectionState.Authenticating -> R.string.connection_state_authenticating
            ConnectionState.Synchronising -> R.string.connection_state_synchronising
            ConnectionState.Connected -> R.string.connection_state_connected
            ConnectionState.Reconnecting -> R.string.connection_state_reconnecting
            ConnectionState.ResynchronisationRequired -> R.string.connection_state_resynchronisation_required
            ConnectionState.AuthenticationFailed -> R.string.connection_state_authentication_failed
            ConnectionState.ProtocolError -> R.string.connection_state_protocol_error
            ConnectionState.TimedOut -> R.string.connection_state_timed_out
            ConnectionState.ServerUnavailable -> R.string.connection_state_server_unavailable
        }

private val SCREEN_PADDING = 16.dp
private val SECTION_SPACING = 20.dp
private val ROW_SPACING = 8.dp

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "待配对浅色", showBackground = true)
@Composable
private fun BurpConnectionScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        BurpConnectionScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "待配对深色", showBackground = true)
@Composable
private fun BurpConnectionScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        BurpConnectionScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "票据过期浅色", showBackground = true)
@Composable
private fun BurpConnectionScreenExpiredLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        BurpConnectionScreen(uiState = previewExpiredState(), onIntent = {})
    }
}

@Preview(name = "票据过期深色", showBackground = true)
@Composable
private fun BurpConnectionScreenExpiredDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        BurpConnectionScreen(uiState = previewExpiredState(), onIntent = {})
    }
}

@Preview(name = "配对被拒浅色", showBackground = true)
@Composable
private fun BurpConnectionScreenRejectedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        BurpConnectionScreen(uiState = previewRejectedState(), onIntent = {})
    }
}

@Preview(name = "配对被拒深色", showBackground = true)
@Composable
private fun BurpConnectionScreenRejectedDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        BurpConnectionScreen(uiState = previewRejectedState(), onIntent = {})
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewState(): BurpConnectionUserInterfaceState =
    BurpConnectionUserInterfaceState(
        connectionState = ConnectionState.Disconnected,
        savedEndpoint = RemoteServerEndpoint(host = "192.0.2.10", port = 9000),
        hostInput = "192.0.2.10",
        portInput = "9000",
        scannedTicket = previewTicket(expiresAt = Instant.parse("2026-09-13T09:00:00Z")),
    )

private fun previewExpiredState(): BurpConnectionUserInterfaceState =
    previewState().copy(ticketRejection = PairingTicketRejection.Expired)

private fun previewRejectedState(): BurpConnectionUserInterfaceState =
    previewState().copy(
        connectionState = ConnectionState.ServerUnavailable,
        pairingOutcome = PairingOutcome.Rejected,
        pairingRejectionReason = PairingRejectionReason.ServerUnreachable,
        pairingConclusion = RemotePairingConclusion.Unreachable,
    )

private fun previewTicket(expiresAt: Instant): PairingTicket =
    PairingTicket(
        protocolVersion = 1,
        host = "192.0.2.10",
        port = 9000,
        challengeIdentifier = PairingChallengeIdentifier(value = "challenge_1"),
        pairingCode = PairingCode(value = "123456"),
        expiresAt = expiresAt,
    )
