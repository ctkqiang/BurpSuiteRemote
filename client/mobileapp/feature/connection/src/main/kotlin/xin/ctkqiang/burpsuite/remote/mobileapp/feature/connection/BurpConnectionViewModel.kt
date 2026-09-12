package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import android.os.Build
import android.util.Log
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
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.PairingCode
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.DEFAULT_REMOTE_PORT
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicket
import xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.PairingTicketDecoder
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.PairingAttempt
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import java.time.Instant

/**
 * 连接屏的状态持有者（plan §55）。
 *
 * 二维码识别放在这里而不是界面里：识别是一件事，界面只负责送来一帧。票据解不出来时如实标记，
 * 不拿默认值补齐（[PairingTicketDecoder] 本身也不做这件事）。
 */
class BurpConnectionViewModel(
    connectionState: Flow<ConnectionState>,
    private val remoteControlClient: RemoteControlClient?,
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
        combine(connectionState, localState) { currentConnectionState, currentLocalState ->
            stateOf(currentConnectionState, currentLocalState)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
            initialValue = stateOf(ConnectionState.Disconnected, localState.value),
        )

    fun handleIntent(intent: BurpConnectionUserInterfaceIntent) {
        when (intent) {
            is BurpConnectionUserInterfaceIntent.UpdatePairingCodeInput ->
                localState.update { state -> state.copy(pairingCodeInput = intent.pairingCodeInput) }

            is BurpConnectionUserInterfaceIntent.OpenPairingScanner -> openScanner()

            is BurpConnectionUserInterfaceIntent.ClosePairingScanner ->
                localState.update { state -> state.copy(isScannerOpen = false) }

            is BurpConnectionUserInterfaceIntent.RequestCameraPermission ->
                effectChannel.trySend(BurpConnectionUserInterfaceEffect.LaunchCameraPermissionRequest)

            is BurpConnectionUserInterfaceIntent.ReportCameraPermission ->
                localState.update { state -> state.copy(cameraPermission = permissionOf(intent.isGranted)) }

            is BurpConnectionUserInterfaceIntent.AnalyzeCameraFrame -> analyzeCameraFrame(intent.imageProxy)

            is BurpConnectionUserInterfaceIntent.SubmitPairing -> submitPairing()

            is BurpConnectionUserInterfaceIntent.RetryConnection -> retryConnection()
        }
    }

    // 面板一打开就申请：权限还没问过时先问一次，被拒过的话由界面给出说明与再次申请入口。
    private fun openScanner() {
        localState.update { state -> state.copy(isScannerOpen = true) }
        if (localState.value.cameraPermission == PairingCameraPermission.Unknown) {
            effectChannel.trySend(BurpConnectionUserInterfaceEffect.LaunchCameraPermissionRequest)
        }
    }

    // 已经拿到票据就不再看后续帧：继续识别只会让相机白耗电。
    private fun analyzeCameraFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isDecodingFrame || localState.value.scannedTicket != null) {
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
                // 只记「解不出来」，不记原文——二维码内容属于用户数据（rules.md §12）。
                Log.i(LOG_TAG, "扫到的二维码不是配对票据")
                null
            }
        localState.update { state ->
            state.copy(
                scannedTicket = ticket,
                hasUndecodableTicket = ticket == null,
                isScannerOpen = false,
            )
        }
    }

    private fun submitPairing() {
        val client = remoteControlClient ?: return
        val ticket = localState.value.scannedTicket ?: return
        val pairingCode =
            localState.value.pairingCodeInput.ifBlank { ticket.pairingCode.value }
        viewModelScope.launch {
            val result =
                client.pair(
                    PairingAttempt(
                        host = ticket.host,
                        port = ticket.port,
                        challengeIdentifier = ticket.challengeIdentifier,
                        pairingCode = PairingCode(value = pairingCode),
                    ),
                )
            localState.update { state -> state.copy(pairingOutcome = outcomeOf(result)) }
        }
    }

    // connect 会一直跑到会话结束，因此整条会话占用一个协程；界面读 connectionState 看进展。
    private fun retryConnection() {
        val client = remoteControlClient ?: return
        val ticket = localState.value.scannedTicket ?: return
        viewModelScope.launch {
            client.connect(
                RemoteConnectionConfiguration(
                    host = ticket.host,
                    port = ticket.port,
                    deviceName = Build.MODEL,
                ),
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (barcodeScannerLazy.isInitialized()) barcodeScanner.close()
    }

    private fun outcomeOf(result: RemoteResult<*>): PairingOutcome =
        when (result) {
            is RemoteResult.Succeeded -> PairingOutcome.Succeeded
            is RemoteResult.Failed -> PairingOutcome.Rejected
        }

    private fun permissionOf(isGranted: Boolean): PairingCameraPermission =
        if (isGranted) PairingCameraPermission.Granted else PairingCameraPermission.Denied

    private fun stateOf(
        currentConnectionState: ConnectionState,
        currentLocalState: LocalPairingState,
    ): BurpConnectionUserInterfaceState {
        val ticket = currentLocalState.scannedTicket
        return BurpConnectionUserInterfaceState(
            connectionState = currentConnectionState,
            serverAddress = ticket?.host,
            serverPort = ticket?.port ?: DEFAULT_REMOTE_PORT,
            pairingCodeInput = currentLocalState.pairingCodeInput,
            isScannerOpen = currentLocalState.isScannerOpen,
            cameraPermission = currentLocalState.cameraPermission,
            scannedTicket = ticket,
            hasUndecodableTicket = currentLocalState.hasUndecodableTicket,
            isScannedTicketExpired =
                ticket?.expiresAt?.let { expiresAt -> expiresAt.isBefore(timeProvider.now()) } ?: false,
            pairingCommandUnavailableReasonResource = unavailableReasonOf(currentLocalState),
            canSubmitPairing =
                remoteControlClient != null &&
                    ticket != null &&
                    (currentLocalState.pairingCodeInput.isNotBlank() || ticket.pairingCode.value.isNotBlank()),
            canRetryConnection = remoteControlClient != null && ticket != null,
            pairingOutcome = currentLocalState.pairingOutcome,
        )
    }

    // 先把「缺什么」排出来，界面照着写原因；两样都不缺就不显示原因。
    private fun unavailableReasonOf(currentLocalState: LocalPairingState): Int? =
        when {
            remoteControlClient == null -> R.string.connection_reason_no_transport
            currentLocalState.scannedTicket == null -> R.string.connection_reason_no_ticket
            else -> null
        }

    private data class LocalPairingState(
        val pairingCodeInput: String = "",
        val isScannerOpen: Boolean = false,
        val cameraPermission: PairingCameraPermission = PairingCameraPermission.Unknown,
        val scannedTicket: PairingTicket? = null,
        val hasUndecodableTicket: Boolean = false,
        val pairingOutcome: PairingOutcome? = null,
    )

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次状态。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L

        // 与主面板一致：方便用 logcat 一起过滤本应用的日志。
        const val LOG_TAG = "BurpRemote"
    }
}
