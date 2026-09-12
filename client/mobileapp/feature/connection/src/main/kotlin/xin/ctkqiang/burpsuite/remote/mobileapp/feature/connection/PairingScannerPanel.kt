package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import androidx.camera.core.Camera
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.ThemeMode
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButton
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteButtonStyle
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteEmptyState
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.design.component.BurpRemoteScannerOverlay
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpRemoteSpacing
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme.BurpsuiteRemoteTheme
import androidx.camera.core.Preview as CameraPreviewUseCase

/**
 * 取景面（plan §55）。
 *
 * 只画取景与权限两件事，不画标题栏也不画返回箭头：它是扫码弹窗的内容，弹窗由装配层用
 * `Dialog(usePlatformDefaultWidth = false)` 承载，因此这里再顶一条 Action Bar 就变成了三层。
 * 退出入口是右上角那一枚悬浮的关闭按钮——没有顶栏，但任何时候都得能退出去。
 *
 * 相机是危险权限，被拒绝时这里给出可读说明与再次申请入口，而不是一片空白；识别本身不在这里做，
 * 只把每一帧交给调用方。取景框铺满可用空间，四角括号、扫描线与手电筒开关由设计系统的
 * [BurpRemoteScannerOverlay] 画在预览之上（暗处扫不出码是最常见的现场问题）。
 */
@Composable
fun PairingScannerPanel(
    cameraPermission: PairingCameraPermission,
    isTorchEnabled: Boolean,
    statusText: String,
    onFrameCaptured: (ImageProxy) -> Unit,
    onToggleTorch: () -> Unit,
    onRequestPermission: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (cameraPermission) {
            PairingCameraPermission.Granted ->
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraViewfinder(
                        isTorchEnabled = isTorchEnabled,
                        onFrameCaptured = onFrameCaptured,
                        modifier = Modifier.fillMaxSize(),
                    )
                    BurpRemoteScannerOverlay(
                        isTorchEnabled = isTorchEnabled,
                        onToggleTorch = onToggleTorch,
                        statusText = statusText,
                    )
                }

            PairingCameraPermission.Denied ->
                PermissionMessage(
                    headlineResource = R.string.connection_scanner_permission_denied_headline,
                    detailResource = R.string.connection_scanner_permission_denied_detail,
                    actionResource = R.string.connection_scanner_permission_request_action,
                    onAction = onRequestPermission,
                )

            PairingCameraPermission.Unknown ->
                PermissionMessage(
                    headlineResource = R.string.connection_scanner_permission_pending_headline,
                    detailResource = R.string.connection_scanner_permission_pending_detail,
                    actionResource = null,
                    onAction = onRequestPermission,
                )
        }

        // 悬浮的关闭入口：不是顶栏，但一个全屏弹窗必须随时能退出去。
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(BurpRemoteSpacing.Large),
        ) {
            BurpRemoteButton(
                text = stringResource(R.string.connection_scanner_close_action),
                onClick = onClose,
                style = BurpRemoteButtonStyle.Ghost,
            )
        }
    }
}

@Composable
private fun PermissionMessage(
    headlineResource: Int,
    detailResource: Int,
    actionResource: Int?,
    onAction: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = BurpRemoteSpacing.Large,
                    vertical = BurpRemoteSpacing.ExtraExtraLarge,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BurpRemoteEmptyState(
            headline = stringResource(headlineResource),
            detail = stringResource(detailResource),
            actionText = actionResource?.let { resource -> stringResource(resource) },
            onAction = actionResource?.let { onAction },
        )
    }
}

/**
 * 相机预览加逐帧分析，并把手电筒状态贴到相机上。
 *
 * 分析用 KEEP_ONLY_LATEST：识别慢过取帧时丢掉旧的帧，而不是排队等着把内存吃满。
 */
@Composable
private fun CameraViewfinder(
    isTorchEnabled: Boolean,
    onFrameCaptured: (ImageProxy) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val currentOnFrameCaptured by rememberUpdatedState(onFrameCaptured)
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

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
                boundCamera =
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis,
                    )
            }
        cameraProviderFuture.addListener(bindListener, analysisExecutor)
        onDispose {
            // 退出弹窗就解绑，否则相机会一直占着，别的应用拿不到。
            if (cameraProviderFuture.isDone) {
                cameraProviderFuture.get().unbindAll()
            }
            boundCamera = null
        }
    }

    // 相机换过实例（解绑再绑）或开关被点时都要重新贴一次；没有闪光灯的设备上这一步会静默失败，
    // 因此界面不去猜「有没有闪光灯」，只如实反映用户的选择。
    LaunchedEffect(isTorchEnabled, boundCamera) {
        boundCamera?.cameraControl?.enableTorch(isTorchEnabled)
    }

    Box(modifier = modifier.fillMaxWidth()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}

// rules.md §8.3：每个屏幕都要有浅色与深色两套预览，否则深色下配色失衡只有装到机器上才发现。
// 已授权那一支会真的去开相机，因此预览只覆盖权限被拒与待授权两种处境。
@Preview(name = "已拒绝浅色", showBackground = true)
@Composable
private fun PairingScannerPanelDeniedLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        PairingScannerPanel(
            cameraPermission = PairingCameraPermission.Denied,
            isTorchEnabled = false,
            statusText = stringResource(R.string.connection_scanner_status_permission_denied),
            onFrameCaptured = {},
            onToggleTorch = {},
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
            isTorchEnabled = true,
            statusText = stringResource(R.string.connection_scanner_status_permission_denied),
            onFrameCaptured = {},
            onToggleTorch = {},
            onRequestPermission = {},
            onClose = {},
        )
    }
}

@Preview(name = "待授权浅色", showBackground = true)
@Composable
private fun PairingScannerPanelPendingLightPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Light) {
        PairingScannerPanel(
            cameraPermission = PairingCameraPermission.Unknown,
            isTorchEnabled = false,
            statusText = stringResource(R.string.connection_scanner_status_pending),
            onFrameCaptured = {},
            onToggleTorch = {},
            onRequestPermission = {},
            onClose = {},
        )
    }
}

@Preview(name = "待授权深色", showBackground = true)
@Composable
private fun PairingScannerPanelPendingDarkPreview() {
    BurpsuiteRemoteTheme(themeMode = ThemeMode.Dark) {
        PairingScannerPanel(
            cameraPermission = PairingCameraPermission.Unknown,
            isTorchEnabled = false,
            statusText = stringResource(R.string.connection_scanner_status_pending),
            onFrameCaptured = {},
            onToggleTorch = {},
            onRequestPermission = {},
            onClose = {},
        )
    }
}
