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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteSectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusPill
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteStatusTone
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTechnicalValue
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteTextField
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.rememberBurpRemoteHaptics
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.technical.colourIn
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.localisation.localisedDateTimeFormatter
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingConclusion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.LocalBurpRemoteDesignTokens
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Burp 连接（plan §43 里设置区的第一项，plan §47 的地址端口，plan §55 的配对流程）。
 *
 * 无状态：状态由外面传进来，用户意图往外抛。扫码面板打开时整屏让给它——取景框太小二维码对不上焦。
 *
 * 每一节只留「标题 + 真正能操作的东西」：说明一律压到 caption 一档的一行，按钮为什么按不动就在
 * 按钮下面那一行里说完。[BurpRemoteEmptyState] 因此不在这块版面上出现——这一屏任何时候都有可读的
 * 状态、可填的输入与可点的动作，不存在「整屏确实没有任何内容」的处境。
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
    // 窗口不参与系统栏的内缩：取景画面是整屏背景，缩进去会在状态栏与手势条两侧露黑边；弹窗内部的
    // 控件层自己按 WindowInsets 让开安全区，因此这里必须让窗口铺满。
    if (uiState.isScannerOpen) {
        Dialog(
            onDismissRequest = { onIntent(BurpConnectionUserInterfaceIntent.ClosePairingScanner) },
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
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

    // 顶栏与底栏由装配层的壳统一提供，各屏不再画第二层标题；区块之间只用一档更大的间距拉开，
    // 不靠把某一节撑满高度来分隔。
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BurpRemoteSpacing.ScreenEdge, vertical = BurpRemoteSpacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.ExtraLarge),
    ) {
        ConnectionStateSection(uiState = uiState)
        PairingTicketSection(
            uiState = uiState,
            onOpenScanner = {
                haptics.tap()
                onIntent(BurpConnectionUserInterfaceIntent.OpenPairingScanner)
            },
        )
        ManualPairingSection(
            uiState = uiState,
            onIntent = onIntent,
            onAction = { haptics.tap() },
        )
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

@Composable
private fun ConnectionStateSection(uiState: BurpConnectionUserInterfaceState) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
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

/**
 * 配对票据：配对的主路径。
 *
 * 扫到就照实显示票据里的那份值；没扫到就把「去扫一张」这个动作直接摆在标题下面，而不是先写一段
 * 解释「为什么现在什么都没有」。用不了的原因分成两行：一行说是什么，一行说下一步做什么。
 */
@Composable
private fun PairingTicketSection(
    uiState: BurpConnectionUserInterfaceState,
    onOpenScanner: () -> Unit,
) {
    val ticket = uiState.scannedTicket
    val rejection = uiState.ticketRejection

    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
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
        } else {
            SectionNote(text = stringResource(R.string.connection_ticket_absent_headline))
            // 还没有票据时正是该说清「去哪儿拿、能拿几次」的时刻：票据在插件那侧只认一次，
            // 配对成功之后同一张二维码就作废了，而用户最容易做错的事就是对着它反复扫。
            SectionNote(text = stringResource(R.string.connection_scan_detail))
        }
        if (rejection != null) {
            SectionNote(
                text = stringResource(ticketRejectionHeadlineResource(rejection)),
                tone = BurpRemoteStatusTone.Danger,
            )
            SectionNote(text = ticketRejectionDetail(uiState = uiState, rejection = rejection))
        }
        BurpRemoteButton(
            text = stringResource(R.string.connection_scan_action),
            onClick = onOpenScanner,
            style = BurpRemoteButtonStyle.Secondary,
        )
    }
}

/** 扫不出来时的兜底：一个输入框、一个主操作，按不动的理由写在按钮下面那一行。 */
@Composable
private fun ManualPairingSection(
    uiState: BurpConnectionUserInterfaceState,
    onIntent: (BurpConnectionUserInterfaceIntent) -> Unit,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_manual_pairing_heading))
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
        pairingNoteResourceOf(uiState = uiState)?.let { noteResource ->
            SectionNote(text = stringResource(noteResource))
        }
    }
}

/**
 * 配对结果。
 *
 * 结论本身就是那一行胶囊——每一种结论都对应不同的下一步；具体原因再补一行，两处分工不重不漏。
 */
@Composable
private fun PairingOutcomeSection(uiState: BurpConnectionUserInterfaceState) {
    val pairingOutcome = uiState.pairingOutcome
    val conclusion = uiState.pairingConclusion
    if (pairingOutcome == null || conclusion == null) return

    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_pairing_result_heading))
        BurpRemoteStatusPill(
            text = stringResource(conclusion.messageResource),
            tone =
                if (pairingOutcome == PairingOutcome.Succeeded) {
                    BurpRemoteStatusTone.Live
                } else {
                    BurpRemoteStatusTone.Danger
                },
        )
        if (pairingOutcome == PairingOutcome.Rejected) {
            SectionNote(
                text = stringResource(pairingRejectionResourceOf(uiState.pairingRejectionReason)),
                tone = BurpRemoteStatusTone.Danger,
            )
            // 结论说的是「哪里不对」，这一条说的是「现在做什么」。只给前者，用户就会对着同一张
            // 已经作废的二维码反复扫——那是这一屏最常见的一次空转。
            conclusion.nextStepResource?.let { resource ->
                SectionNote(text = stringResource(resource))
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
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
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_endpoint_heading))
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
        SectionNote(text = stringResource(R.string.connection_endpoint_default_port_hint, DEFAULT_REMOTE_PORT))
        BurpRemoteButton(
            text = stringResource(R.string.connection_endpoint_save_action),
            onClick = {
                onAction()
                onIntent(BurpConnectionUserInterfaceIntent.SaveEndpoint)
            },
            style = BurpRemoteButtonStyle.Secondary,
            isEnabled = uiState.isEndpointSavable,
        )
        // 空字段不必解释，填错格式才需要：端口这一行只在值本身不合法时才出来。
        if (uiState.hasInvalidPort) {
            SectionNote(
                text = stringResource(R.string.connection_endpoint_invalid_port_hint),
                tone = BurpRemoteStatusTone.Warning,
            )
        }
    }
}

@Composable
private fun RetrySection(
    uiState: BurpConnectionUserInterfaceState,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BurpRemoteSpacing.Small)) {
        BurpRemoteSectionHeading(text = stringResource(R.string.connection_retry_heading))
        BurpRemoteButton(
            text = stringResource(R.string.connection_retry_action),
            onClick = onRetry,
            style = BurpRemoteButtonStyle.Secondary,
            isEnabled = uiState.canRetryConnection && uiState.unavailableReasonResource == null,
        )
        retryNoteResourceOf(uiState = uiState)?.let { noteResource ->
            SectionNote(text = stringResource(noteResource))
        }
    }
}

/**
 * 标题下面、控件旁边的一行说明。
 *
 * 只承载一句话：为什么这个按钮按不动、这张票据为什么用不了。整段解释原先由空状态承载，于是每一节
 * 都先来一堵文字墙；压到 caption 一档之后，正文与控件才是版面上的主角。
 *
 * @param text 要说的那一句话。
 * @param tone 语气，决定字色；默认中性，只有需要用户立刻处理时才换成危险或警告档。
 */
@Composable
private fun SectionNote(
    text: String,
    tone: BurpRemoteStatusTone = BurpRemoteStatusTone.Neutral,
) {
    val tokens = LocalBurpRemoteDesignTokens.current

    BurpRemoteText(
        text = text,
        style = tokens.typography.caption,
        colour = tone.colourIn(tokens.colourScheme),
    )
}

/** 配对按钮为什么按不动：正在飞的配对最先说，其次才是传输层与票据那两类原因。 */
@StringRes
private fun pairingNoteResourceOf(uiState: BurpConnectionUserInterfaceState): Int? =
    when {
        uiState.isPairingInFlight -> R.string.connection_pairing_in_flight
        else -> uiState.unavailableReasonResource
    }

/** 连接按钮为什么按不动：先说要先有什么，再说传输层此刻能不能发命令。 */
@StringRes
private fun retryNoteResourceOf(uiState: BurpConnectionUserInterfaceState): Int? =
    when {
        !uiState.canRetryConnection -> R.string.connection_retry_disabled_hint
        uiState.unavailableReasonResource != null -> uiState.unavailableReasonResource
        else -> null
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
private val TICKET_EXPIRY_FORMATTER: DateTimeFormatter
    get() = localisedDateTimeFormatter(FormatStyle.MEDIUM)

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
        pairingTextInput = "BURP-PAIR-1-192.0.2.10-9000",
        scannedTicket = previewTicket(expiresAt = Instant.parse("2026-09-13T09:00:00Z")),
        // 这个构建里配对链路还没接上：按钮因此不可用，而原因就写在按钮下面那一行。
        unavailableReasonResource = R.string.connection_reason_no_transport,
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
