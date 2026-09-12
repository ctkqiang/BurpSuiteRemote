package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingChallengeIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ConnectionStateIndicator
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LabelValueText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.LiveOfflineBadge
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.NotImplementedReasonText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.SectionHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Burp 连接（plan §43 里设置区的第一项）。无状态：状态由外面传进来，用户意图往外抛。 */
@Composable
fun BurpConnectionScreen(
    uiState: BurpConnectionUserInterfaceState,
    onIntent: (BurpConnectionUserInterfaceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 扫码面板占满整屏：取景框太小的话二维码对不上焦，反而扫不出来。
    if (uiState.isScannerOpen) {
        PairingScannerPanel(
            cameraPermission = uiState.cameraPermission,
            hasUndecodableTicket = uiState.hasUndecodableTicket,
            onFrameCaptured = { imageProxy ->
                onIntent(BurpConnectionUserInterfaceIntent.AnalyzeCameraFrame(imageProxy))
            },
            onRequestPermission = { onIntent(BurpConnectionUserInterfaceIntent.RequestCameraPermission) },
            onClose = { onIntent(BurpConnectionUserInterfaceIntent.ClosePairingScanner) },
            modifier = modifier,
        )
        return
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(SECTION_PADDING),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
        ) {
            ScreenHeading(titleResource = R.string.connection_title)
            ConnectionSummary(uiState = uiState)
            PairingTicketSection(uiState = uiState)
            ScanPairingEntry(onIntent = onIntent)
            ManualPairingEntry(uiState = uiState, onIntent = onIntent)
            RetryEntry(uiState = uiState, onIntent = onIntent)
        }
    }
}

@Composable
private fun ConnectionSummary(uiState: BurpConnectionUserInterfaceState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CARD_PADDING),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            SectionHeading(titleResource = R.string.connection_status_heading)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveOfflineBadge(connectionState = uiState.connectionState)
                Spacer(modifier = Modifier.width(ROW_SPACING))
                ConnectionStateIndicator(connectionState = uiState.connectionState)
            }
            LabelValueText(
                labelResource = R.string.connection_server_address_label,
                value = uiState.serverAddress ?: stringResource(R.string.connection_server_address_absent),
            )
            LabelValueText(
                labelResource = R.string.connection_server_port_label,
                value = uiState.serverPort.toString(),
            )
            uiState.pairingOutcome?.let { pairingOutcome -> PairingOutcomeText(pairingOutcome = pairingOutcome) }
        }
    }
}

@Composable
private fun PairingOutcomeText(pairingOutcome: PairingOutcome) {
    Text(
        text =
            stringResource(
                when (pairingOutcome) {
                    PairingOutcome.Succeeded -> R.string.connection_pairing_succeeded
                    PairingOutcome.Rejected -> R.string.connection_pairing_rejected
                },
            ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PairingTicketSection(uiState: BurpConnectionUserInterfaceState) {
    val ticket = uiState.scannedTicket
    if (ticket == null) {
        if (uiState.hasUndecodableTicket) {
            EmptyStateText(messageResource = R.string.connection_ticket_undecodable)
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.connection_ticket_heading)
        LabelValueText(labelResource = R.string.connection_ticket_host_label, value = ticket.host)
        LabelValueText(
            labelResource = R.string.connection_ticket_port_label,
            value = ticket.port.toString(),
        )
        LabelValueText(
            labelResource = R.string.connection_ticket_protocol_version_label,
            value = ticket.protocolVersion.toString(),
        )
        LabelValueText(
            labelResource = R.string.connection_ticket_expires_at_label,
            value = TICKET_EXPIRY_FORMATTER.format(ticket.expiresAt),
        )
        if (uiState.isScannedTicketExpired) {
            EmptyStateText(messageResource = R.string.connection_ticket_expired)
        }
    }
}

@Composable
private fun ScanPairingEntry(onIntent: (BurpConnectionUserInterfaceIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.connection_scan_pairing_title)
        Button(
            onClick = { onIntent(BurpConnectionUserInterfaceIntent.OpenPairingScanner) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.connection_scan_pairing_action))
        }
    }
}

@Composable
private fun ManualPairingEntry(
    uiState: BurpConnectionUserInterfaceState,
    onIntent: (BurpConnectionUserInterfaceIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.connection_manual_pairing_title)
        Text(
            text = stringResource(R.string.connection_manual_pairing_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.pairingCodeInput,
            onValueChange = { pairingCodeInput ->
                onIntent(BurpConnectionUserInterfaceIntent.UpdatePairingCodeInput(pairingCodeInput))
            },
            label = { Text(text = stringResource(R.string.connection_manual_pairing_code_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onIntent(BurpConnectionUserInterfaceIntent.SubmitPairing) },
            enabled = uiState.canSubmitPairing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.connection_manual_pairing_action))
        }
        UnavailableReason(resource = uiState.pairingCommandUnavailableReasonResource)
    }
}

@Composable
private fun RetryEntry(
    uiState: BurpConnectionUserInterfaceState,
    onIntent: (BurpConnectionUserInterfaceIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        SectionHeading(titleResource = R.string.connection_retry_heading)
        Button(
            onClick = { onIntent(BurpConnectionUserInterfaceIntent.RetryConnection) },
            enabled = uiState.canRetryConnection,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.connection_retry_action))
        }
        UnavailableReason(resource = uiState.pairingCommandUnavailableReasonResource)
    }
}

@Composable
private fun UnavailableReason(
    @StringRes resource: Int?,
) {
    if (resource != null) {
        NotImplementedReasonText(reasonResource = resource)
    }
}

// 票据失效时刻按设备时区与当前语言格式化：用户要拿它跟插件界面上显示的时间对齐。
private val TICKET_EXPIRY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "浅色", showBackground = true)
@Composable
private fun BurpConnectionScreenLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        BurpConnectionScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "深色", showBackground = true)
@Composable
private fun BurpConnectionScreenDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        BurpConnectionScreen(uiState = previewState(), onIntent = {})
    }
}

@Preview(name = "空态浅色", showBackground = true)
@Composable
private fun BurpConnectionScreenEmptyLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        BurpConnectionScreen(uiState = BurpConnectionUserInterfaceState(), onIntent = {})
    }
}

@Preview(name = "空态深色", showBackground = true)
@Composable
private fun BurpConnectionScreenEmptyDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        BurpConnectionScreen(
            uiState =
                BurpConnectionUserInterfaceState(
                    connectionState = ConnectionState.ServerUnavailable,
                    pairingCommandUnavailableReasonResource = R.string.connection_reason_no_transport,
                ),
            onIntent = {},
        )
    }
}

// 预览样本：只在 @Preview 里用，正式界面一律读领域端口。
private fun previewState(): BurpConnectionUserInterfaceState =
    BurpConnectionUserInterfaceState(
        connectionState = ConnectionState.Disconnected,
        serverAddress = "192.0.2.10",
        serverPort = 9000,
        pairingCodeInput = "",
        scannedTicket =
            PairingTicket(
                protocolVersion = 1,
                host = "192.0.2.10",
                port = 9000,
                challengeIdentifier = PairingChallengeIdentifier(value = "challenge_1"),
                pairingCode = PairingCode(value = "123456"),
                expiresAt = Instant.parse("2026-09-13T09:00:00Z"),
            ),
        pairingCommandUnavailableReasonResource = R.string.connection_reason_no_transport,
    )

private val SECTION_PADDING = 24.dp
private val CARD_PADDING = 12.dp
private val ROW_SPACING = 8.dp
private val SECTION_SPACING = 24.dp
