package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.EmptyStateText
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.components.ScreenHeading
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import androidx.camera.core.Preview as CameraPreviewUseCase

/**
 * 扫码面板（plan §55）。
 *
 * 相机是危险权限，被拒绝时这里给出可读说明与再次申请入口，而不是一片空白；
 * 识别本身不在这里做——面板只把每一帧交给 ViewModel，怎么解由它决定。
 */
@Composable
fun PairingScannerPanel(
    cameraPermission: PairingCameraPermission,
    hasUndecodableTicket: Boolean,
    onFrameCaptured: (ImageProxy) -> Unit,
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(PANEL_PADDING),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            ScreenHeading(titleResource = R.string.connection_scan_pairing_title)
            if (hasUndecodableTicket) {
                EmptyStateText(messageResource = R.string.connection_ticket_undecodable)
            }
            when (cameraPermission) {
                PairingCameraPermission.Granted ->
                    CameraPreview(
                        onFrameCaptured = onFrameCaptured,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )

                PairingCameraPermission.Denied -> CameraPermissionExplanation(onRequestPermission = onRequestPermission)

                PairingCameraPermission.Unknown ->
                    EmptyStateText(messageResource = R.string.connection_scanner_permission_pending)
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.connection_scanner_close_action))
            }
        }
    }
}

@Composable
private fun CameraPermissionExplanation(onRequestPermission: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
        Text(
            text = stringResource(R.string.connection_scanner_permission_denied),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.connection_scanner_permission_request_action))
        }
    }
}

/**
 * 相机预览加逐帧分析。
 *
 * 分析用 KEEP_ONLY_LATEST：识别慢过取帧时丢掉旧的帧，而不是排队等着把内存吃满。
 */
@Composable
private fun CameraPreview(
    onFrameCaptured: (ImageProxy) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val currentOnFrameCaptured by rememberUpdatedState(onFrameCaptured)

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val analysisExecutor = ContextCompat.getMainExecutor(context)
        val bindListener =
            Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview =
                    CameraPreviewUseCase.Builder().build().also { cameraPreview ->
                        cameraPreview.surfaceProvider = previewView.surfaceProvider
                    }
                val imageAnalysis =
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                currentOnFrameCaptured(imageProxy)
                            }
                        }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }
        cameraProviderFuture.addListener(bindListener, analysisExecutor)
        onDispose {
            // 回到别的屏就解绑，否则相机会一直占着，别的应用拿不到。
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
@Preview(name = "已拒绝浅色", showBackground = true)
@Composable
private fun PairingScannerPanelDeniedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        PairingScannerPanel(
            cameraPermission = PairingCameraPermission.Denied,
            hasUndecodableTicket = false,
            onFrameCaptured = {},
            onRequestPermission = {},
            onClose = {},
        )
    }
}

@Preview(name = "待授权浅色", showBackground = true)
@Composable
private fun PairingScannerPanelUnknownLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        PairingScannerPanel(
            cameraPermission = PairingCameraPermission.Unknown,
            hasUndecodableTicket = false,
            onFrameCaptured = {},
            onRequestPermission = {},
            onClose = {},
        )
    }
}

@Preview(name = "已拒绝深色", showBackground = true)
@Composable
private fun PairingScannerPanelDeniedDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        PairingScannerPanel(
            cameraPermission = PairingCameraPermission.Denied,
            hasUndecodableTicket = true,
            onFrameCaptured = {},
            onRequestPermission = {},
            onClose = {},
        )
    }
}

@Preview(name = "待授权深色", showBackground = true)
@Composable
private fun PairingScannerPanelUnknownDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        PairingScannerPanel(
            cameraPermission = PairingCameraPermission.Unknown,
            hasUndecodableTicket = false,
            onFrameCaptured = {},
            onRequestPermission = {},
            onClose = {},
        )
    }
}

private val PANEL_PADDING = 24.dp
private val ROW_SPACING = 12.dp
