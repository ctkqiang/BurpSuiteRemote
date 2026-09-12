package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import android.os.Build
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicketDecoder
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.RemoteProtocolVersion
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.RemoteServerEndpoint
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingConclusion
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingCoordinator
import java.time.Instant

/**
 * 连接屏的状态持有者（plan §47 的连接配置、plan §55 的配对流程）。
 *
 * 二维码识别放在这里而不是界面里：识别是一件事，界面只负责送来一帧。票据解不出来、过期或版本
 * 不符时如实分类，界面照着写出「下一步该做什么」，而不是笼统来一句扫码失败。
 *
 * 配对一律走 [RemotePairingCoordinator]：扫码得到的文本与手输的文本走同一条链路，于是不存在
 * 「扫码能连、手输连不上」这种差异。
 *
 * 日志只写地址、端口、版本与结论：配对码与票据原文是凭据，绝不进日志（rules.md §12）。
 */
class BurpConnectionViewModel(
    connectionState: Flow<ConnectionState>,
    private val settingsRepository: SettingsRepository,
    private val remotePairingCoordinator: RemotePairingCoordinator?,
    private val remoteControlClient: RemoteControlClient?,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
    private val timeProvider: TimeProvider = TimeProvider { Instant.now() },
) : ViewModel() {
    private val effectChannel = Channel<BurpConnectionUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；权限弹窗这类动作走这里，不塞进状态。 */
    val effects: Flow<BurpConnectionUserInterfaceEffect> = effectChannel.receiveAsFlow()

    private val localState = MutableStateFlow(LocalPairingState())

    // 只有二维码一种码制：留着全部码制会让识别变慢，而这里只可能扫到票据。
    private val barcodeScannerLazy: Lazy<BarcodeScanner> =
        lazy {
            BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
            )
        }

    private val barcodeScanner: BarcodeScanner by barcodeScannerLazy

    private var isDecodingFrame = false

    val uiState: StateFlow<BurpConnectionUserInterfaceState> =
        combine(
            connectionState,
            localState,
            settingsRepository.observeServerEndpoint(),
            settingsRepository.observeIsPaired(),
        ) { currentConnectionState, currentLocalState, savedEndpoint, isPaired ->
            stateOf(
                currentConnectionState = currentConnectionState,
                currentLocalState = currentLocalState,
                savedEndpoint = savedEndpoint,
                isPaired = isPaired,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
            initialValue = BurpConnectionUserInterfaceState(),
        )

    fun handleIntent(intent: BurpConnectionUserInterfaceIntent) {
        when (intent) {
            is BurpConnectionUserInterfaceIntent.UpdateHostInput -> updateHostInput(intent.hostInput)

            is BurpConnectionUserInterfaceIntent.UpdatePortInput -> updatePortInput(intent.portInput)

            is BurpConnectionUserInterfaceIntent.UpdatePairingTextInput ->
                localState.update { state -> state.copy(pairingTextInput = intent.pairingTextInput) }

            is BurpConnectionUserInterfaceIntent.SaveEndpoint -> saveEndpoint()

            is BurpConnectionUserInterfaceIntent.StartPairing -> startPairing()

            is BurpConnectionUserInterfaceIntent.PairTicketText -> pairWithTicketText(intent.ticketText)

            is BurpConnectionUserInterfaceIntent.OpenPairingScanner -> openScanner()

            is BurpConnectionUserInterfaceIntent.ClosePairingScanner -> closeScanner()

            is BurpConnectionUserInterfaceIntent.ToggleScannerTorch -> toggleTorch()

            is BurpConnectionUserInterfaceIntent.RequestCameraPermission ->
                effectChannel.trySend(BurpConnectionUserInterfaceEffect.LaunchCameraPermissionRequest)

            is BurpConnectionUserInterfaceIntent.ReportCameraPermission -> reportCameraPermission(intent.isGranted)

            is BurpConnectionUserInterfaceIntent.AnalyzeCameraFrame -> analyzeCameraFrame(intent.imageProxy)

            is BurpConnectionUserInterfaceIntent.RetryConnection -> retryConnection()

            is BurpConnectionUserInterfaceIntent.ClearPairing -> clearPairing()
        }
    }

    // 用户一动手就不再让已保存的值回填，否则每敲一个字符都会被旧值顶掉。
    private fun updateHostInput(hostInput: String) {
        localState.update { state -> state.copy(hostInput = hostInput, hasEditedEndpoint = true) }
    }

    private fun updatePortInput(portInput: String) {
        // 只收数字：端口输入框里出现字母只可能是误触，让它留在框里再报错不如直接不收。
        val digitsOnly = portInput.filter { character -> character.isDigit() }
        localState.update { state -> state.copy(portInput = digitsOnly, hasEditedEndpoint = true) }
    }

    // 面板一打开就申请：权限还没问过时先问一次，被拒过的话由界面给出说明与再次申请入口。
    private fun openScanner() {
        record(category = TechnicalLogCategory.Pairing, message = "扫码面板打开")
        localState.update { state -> state.copy(isScannerOpen = true) }
        if (localState.value.cameraPermission == PairingCameraPermission.Unknown) {
            effectChannel.trySend(BurpConnectionUserInterfaceEffect.LaunchCameraPermissionRequest)
        }
    }

    private fun closeScanner() {
        record(category = TechnicalLogCategory.Pairing, message = "扫码面板关闭")
        localState.update { state -> state.copy(isScannerOpen = false, isTorchEnabled = false) }
    }

    private fun toggleTorch() {
        val isTorchEnabled = !localState.value.isTorchEnabled
        record(
            category = TechnicalLogCategory.Pairing,
            message = "扫码补光已切换",
            attributes = mapOf("isTorchEnabled" to isTorchEnabled.toString()),
        )
        localState.update { state -> state.copy(isTorchEnabled = isTorchEnabled) }
    }

    private fun reportCameraPermission(isGranted: Boolean) {
        record(
            category = TechnicalLogCategory.Pairing,
            message = if (isGranted) "相机权限已授予" else "相机权限被拒绝，改为引导用户手输配对文本",
        )
        localState.update { state ->
            state.copy(
                cameraPermission = if (isGranted) PairingCameraPermission.Granted else PairingCameraPermission.Denied,
            )
        }
    }

    // 已经拿到票据原文就不再看后续帧：继续识别只会让相机白耗电。
    private fun analyzeCameraFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isDecodingFrame || localState.value.scannedTicketText != null) {
            imageProxy.close()
            return
        }
        isDecodingFrame = true
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner
            .process(inputImage)
            .addOnSuccessListener { barcodes -> applyDecodedBarcodes(barcodes) }
            .addOnCompleteListener {
                imageProxy.close()
                isDecodingFrame = false
            }
    }

    private fun applyDecodedBarcodes(barcodes: List<Barcode>) {
        val encodedTicketText = barcodes.firstNotNullOfOrNull { barcode -> barcode.rawValue } ?: return
        val ticket =
            try {
                PairingTicketDecoder.decodeFromText(encodedTicketText)
            } catch (malformedTicket: IllegalArgumentException) {
                // 二维码里可能是任意一张名片、一条链接；解不出来是正常结果，不是故障。
                // 只记异常类型，不记原文——二维码内容属于用户数据（rules.md §12）。
                record(
                    category = TechnicalLogCategory.Pairing,
                    message = "扫到的二维码不是配对票据",
                    attributes = mapOf("failure" to malformedTicket::class.java.simpleName),
                )
                null
            }
        record(
            category = TechnicalLogCategory.Pairing,
            message = if (ticket == null) "识别结果：内容不符合票据契约" else "识别结果：报文合法，本地校验通过后再交给配对链路",
            attributes =
                if (ticket == null) {
                    emptyMap()
                } else {
                    mapOf(
                        "host" to ticket.host,
                        "port" to ticket.port.toString(),
                        "protocolVersion" to ticket.protocolVersion.toString(),
                    )
                },
        )
        localState.update { state ->
            state.copy(
                scannedTicketText = encodedTicketText,
                scannedTicket = ticket,
                hasUndecodableTicket = ticket == null,
                isScannerOpen = false,
                isTorchEnabled = false,
                pairingOutcome = null,
                pairingRejectionReason = null,
                pairingConclusion = null,
            )
        }
    }

    // 扫码得到的文本与手输的文本在这里合流：谁有内容用谁，手输优先——用户刚敲进去的那份才是他的本意。
    private fun startPairing() {
        val coordinator = remotePairingCoordinator ?: return
        val currentLocalState = localState.value
        val encodedTicketText =
            currentLocalState.pairingTextInput.ifBlank { currentLocalState.scannedTicketText.orEmpty() }
        if (encodedTicketText.isBlank()) return

        record(
            category = TechnicalLogCategory.Pairing,
            message = "配对请求交给配对链路",
            attributes =
                mapOf(
                    "source" to if (currentLocalState.pairingTextInput.isNotBlank()) "manual" else "scanner",
                ),
        )
        localState.update { state ->
            state.copy(
                isPairingInFlight = true,
                pairingOutcome = null,
                pairingRejectionReason = null,
                pairingConclusion = null,
            )
        }
        viewModelScope.launch {
            applyConclusion(coordinator.pairWithEncodedTicket(encodedTicketText))
        }
    }

    // 调试入口：把外部票据当成「刚扫到」的那一份，链路与扫码完全一致，只是少了相机那一步。
    private fun pairWithTicketText(ticketText: String) {
        val ticket =
            try {
                PairingTicketDecoder.decodeFromText(ticketText)
            } catch (malformedTicket: IllegalArgumentException) {
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "调试注入的文本解不成票据，按无效票据处理",
                    attributes = mapOf("failure" to malformedTicket::class.java.simpleName),
                )
                null
            }
        record(
            category = TechnicalLogCategory.Pairing,
            message = "调试注入入口：跳过相机，直接使用外部票据",
            attributes =
                if (ticket == null) {
                    emptyMap()
                } else {
                    // 只记地址、端口、协议版本与失效时刻；配对码与会话标识属于凭据（rules.md §12）。
                    mapOf(
                        "host" to ticket.host,
                        "port" to ticket.port.toString(),
                        "protocolVersion" to ticket.protocolVersion.toString(),
                        "expiresAt" to ticket.expiresAt.toString(),
                    )
                },
        )
        localState.update { state ->
            state.copy(
                pairingTextInput = ticketText,
                scannedTicketText = ticketText,
                scannedTicket = ticket,
                hasUndecodableTicket = ticket == null,
                isScannerOpen = false,
                isTorchEnabled = false,
                pairingOutcome = null,
                pairingRejectionReason = null,
                pairingConclusion = null,
            )
        }
        startPairing()
    }

    private fun applyConclusion(conclusion: RemotePairingConclusion) {
        val isSucceeded = conclusion == RemotePairingConclusion.Succeeded
        val outcome = if (isSucceeded) PairingOutcome.Succeeded else PairingOutcome.Rejected
        val logCategory =
            if (isSucceeded) TechnicalLogCategory.Pairing else TechnicalLogCategory.Failure
        record(
            category = logCategory,
            message = if (isSucceeded) "配对结论：插件已接受本机" else "配对结论：未成功，界面照具体原因写下一步",
            attributes = mapOf("conclusion" to conclusion.name),
        )
        localState.update { state ->
            state.copy(
                isPairingInFlight = false,
                pairingOutcome = outcome,
                pairingRejectionReason = rejectionReasonOf(conclusion),
                // 配对成功之后地址端口由票据那份值说了算，手输的旧值不再回填。
                hasEditedEndpoint = !isSucceeded && state.hasEditedEndpoint,
                pairingConclusion = conclusion,
            )
        }
    }

    private fun saveEndpoint() {
        val state = uiState.value
        val port = state.parsedPortInput
        if (port == null || state.hostInput.isBlank()) {
            record(
                category = TechnicalLogCategory.Failure,
                message = "地址或端口不合法，没有保存",
                attributes = mapOf("host" to state.hostInput, "portInput" to state.portInput),
            )
            return
        }
        record(
            category = TechnicalLogCategory.Transport,
            message = "用户保存了插件端点",
            attributes = mapOf("host" to state.hostInput, "port" to port.toString()),
        )
        viewModelScope.launch {
            settingsRepository.saveServerEndpoint(RemoteServerEndpoint(host = state.hostInput, port = port))
        }
    }

    // connect 会一直跑到会话结束，因此整条会话占用一个协程；界面读 connectionState 看进展。
    private fun retryConnection() {
        val client = remoteControlClient ?: return
        val endpoint = uiState.value.savedEndpoint ?: return
        record(
            category = TechnicalLogCategory.Transport,
            message = "发起连接",
            attributes =
                mapOf(
                    "host" to endpoint.host,
                    "port" to endpoint.port.toString(),
                    "deviceName" to Build.MODEL.orEmpty(),
                ),
        )
        viewModelScope.launch {
            client.connect(
                RemoteConnectionConfiguration(
                    host = endpoint.host,
                    port = endpoint.port,
                    deviceName = Build.MODEL,
                ),
            )
            record(
                category = TechnicalLogCategory.Transport,
                message = "连接会话已结束",
                attributes = mapOf("connectionState" to uiState.value.connectionState.toString()),
            )
        }
    }

    private fun clearPairing() {
        record(category = TechnicalLogCategory.Pairing, message = "清除配对：设备身份与已保存的地址端口一并抹掉")
        viewModelScope.launch {
            remoteControlClient?.disconnect()
            settingsRepository.clearPairing()
            localState.update { state ->
                state.copy(
                    hasEditedEndpoint = false,
                    hostInput = "",
                    portInput = "",
                    pairingTextInput = "",
                    scannedTicketText = null,
                    scannedTicket = null,
                    hasUndecodableTicket = false,
                    isPairingInFlight = false,
                    pairingOutcome = null,
                    pairingRejectionReason = null,
                    pairingConclusion = null,
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (barcodeScannerLazy.isInitialized()) barcodeScanner.close()
    }

    // 归类只决定状态标签与色调；具体原因由结论自己的文案给出，两者分工不重不漏。
    private fun rejectionReasonOf(conclusion: RemotePairingConclusion): PairingRejectionReason? =
        when (conclusion) {
            RemotePairingConclusion.Succeeded -> null
            RemotePairingConclusion.TicketExpired,
            RemotePairingConclusion.PairingSessionExpired,
            -> PairingRejectionReason.PairingSessionExpired

            RemotePairingConclusion.PairingCodeMismatch -> PairingRejectionReason.PairingCodeRejected

            RemotePairingConclusion.ProtocolVersionUnsupported -> PairingRejectionReason.ProtocolVersionUnsupported

            RemotePairingConclusion.DeviceNotPaired -> PairingRejectionReason.AuthenticationRejected

            RemotePairingConclusion.Unreachable -> PairingRejectionReason.ServerUnreachable

            RemotePairingConclusion.MalformedTicket,
            RemotePairingConclusion.Failed,
            -> PairingRejectionReason.Unknown
        }

    private fun stateOf(
        currentConnectionState: ConnectionState,
        currentLocalState: LocalPairingState,
        savedEndpoint: RemoteServerEndpoint?,
        isPaired: Boolean,
    ): BurpConnectionUserInterfaceState {
        val ticket = currentLocalState.scannedTicket
        val ticketRejection = ticketRejectionOf(ticket = ticket, localState = currentLocalState)
        // 没动过输入框时显示已保存的那份值，动过之后以用户手里的为准。
        val hostInput =
            if (currentLocalState.hasEditedEndpoint) currentLocalState.hostInput else savedEndpoint?.host.orEmpty()
        val portInput =
            if (currentLocalState.hasEditedEndpoint) {
                currentLocalState.portInput
            } else {
                savedEndpoint?.port?.toString().orEmpty()
            }
        return BurpConnectionUserInterfaceState(
            connectionState = currentConnectionState,
            savedEndpoint = savedEndpoint,
            isPaired = isPaired,
            hostInput = hostInput,
            portInput = portInput,
            pairingTextInput = currentLocalState.pairingTextInput,
            isScannerOpen = currentLocalState.isScannerOpen,
            cameraPermission = currentLocalState.cameraPermission,
            isTorchEnabled = currentLocalState.isTorchEnabled,
            scannedTicket = ticket,
            ticketRejection = ticketRejection,
            supportedProtocolVersion = RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION,
            unavailableReasonResource = pairingUnavailableReasonOf(currentLocalState, ticketRejection),
            isPairingInFlight = currentLocalState.isPairingInFlight,
            pairingOutcome = currentLocalState.pairingOutcome,
            pairingRejectionReason = currentLocalState.pairingRejectionReason,
            pairingConclusion = currentLocalState.pairingConclusion,
        )
    }

    // 先把「缺什么」排出来，界面照着写原因；什么都不缺就不显示原因。
    private fun pairingUnavailableReasonOf(
        localState: LocalPairingState,
        ticketRejection: PairingTicketRejection?,
    ): Int? =
        when {
            remotePairingCoordinator == null || remoteControlClient == null -> R.string.connection_reason_no_transport
            localState.pairingTextInput.isNotBlank() -> null
            ticketRejection != null -> R.string.connection_reason_ticket_unusable
            else -> R.string.connection_reason_no_ticket
        }

    // 「解不出来」是扫到了一张别的码；「过期」与「版本不符」是票据本身的问题，三者要做的事不同。
    private fun ticketRejectionOf(
        ticket: PairingTicket?,
        localState: LocalPairingState,
    ): PairingTicketRejection? =
        when {
            localState.hasUndecodableTicket -> PairingTicketRejection.Undecodable
            ticket == null -> null
            ticket.protocolVersion != RemoteProtocolVersion.CURRENT_PROTOCOL_VERSION ->
                PairingTicketRejection.ProtocolVersionUnsupported

            !ticket.expiresAt.isAfter(timeProvider.now()) -> PairingTicketRejection.Expired
            else -> null
        }

    private fun record(
        category: TechnicalLogCategory,
        message: String,
        attributes: Map<String, String> = emptyMap(),
    ) {
        technicalLog.record(
            TechnicalLogEvent(category = category, message = message, attributes = attributes),
        )
    }

    private data class LocalPairingState(
        val pairingTextInput: String = "",
        val hostInput: String = "",
        val portInput: String = "",
        val hasEditedEndpoint: Boolean = false,
        val isScannerOpen: Boolean = false,
        val cameraPermission: PairingCameraPermission = PairingCameraPermission.Unknown,
        val isTorchEnabled: Boolean = false,
        val scannedTicketText: String? = null,
        val scannedTicket: PairingTicket? = null,
        val hasUndecodableTicket: Boolean = false,
        val isPairingInFlight: Boolean = false,
        val pairingOutcome: PairingOutcome? = null,
        val pairingRejectionReason: PairingRejectionReason? = null,
        val pairingConclusion: RemotePairingConclusion? = null,
    )

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次状态。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
