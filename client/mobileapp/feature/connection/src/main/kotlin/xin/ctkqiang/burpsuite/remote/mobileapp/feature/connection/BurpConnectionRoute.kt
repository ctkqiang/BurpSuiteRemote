package xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings.SettingsRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemotePairingCoordinator

/**
 * 装配层：把连接状态、设置端口与配对入口交给 ViewModel，把状态与意图处理交给无状态的界面。
 *
 * 权限弹窗只能在这一层发起（`rememberLauncherForActivityResult` 是 Compose 的东西），
 * 因此 ViewModel 只发「要申请了」这个效果，实际弹窗由这里执行，结果再作为意图回给 ViewModel。
 *
 * [shouldOpenScannerOnEntry] 给顶栏的扫码动作用：壳层把用户直接送进这一屏，而「落地即开面板」
 * 只有本屏能做——相机权限此刻是什么处境，只有它知道。
 *
 * @param injectedPairingTicketText 调试入口递进来的票据文本；给了它就跳过相机直接配对，正式包拿不到它。
 */
@Composable
fun BurpConnectionRoute(
    connectionState: Flow<ConnectionState>,
    settingsRepository: SettingsRepository,
    remotePairingCoordinator: RemotePairingCoordinator?,
    remoteControlClient: RemoteControlClient?,
    shouldOpenScannerOnEntry: Boolean = false,
    technicalLog: TechnicalLog = SilentTechnicalLog,
    modifier: Modifier = Modifier,
    injectedPairingTicketText: String? = null,
) {
    val viewModel: BurpConnectionViewModel =
        viewModel {
            BurpConnectionViewModel(
                connectionState = connectionState,
                settingsRepository = settingsRepository,
                remotePairingCoordinator = remotePairingCoordinator,
                remoteControlClient = remoteControlClient,
                technicalLog = technicalLog,
            )
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.handleIntent(BurpConnectionUserInterfaceIntent.ReportCameraPermission(isGranted))
        }

    // 系统里已经授权时不必再弹一次窗：先把这个事实告诉 ViewModel，界面才能直接开预览。
    LaunchedEffect(context) {
        val isAlreadyGranted =
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (isAlreadyGranted) {
            viewModel.handleIntent(BurpConnectionUserInterfaceIntent.ReportCameraPermission(true))
        }
    }

    LaunchedEffect(viewModel, shouldOpenScannerOnEntry) {
        if (shouldOpenScannerOnEntry) {
            viewModel.handleIntent(BurpConnectionUserInterfaceIntent.OpenPairingScanner)
        }
    }

    // 注入的票据走与扫码完全相同的那条链路：这里只是把文本递给 ViewModel，没有第二条配对路径。
    LaunchedEffect(viewModel, injectedPairingTicketText) {
        if (injectedPairingTicketText != null) {
            viewModel.handleIntent(BurpConnectionUserInterfaceIntent.PairTicketText(injectedPairingTicketText))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BurpConnectionUserInterfaceEffect.LaunchCameraPermissionRequest ->
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    BurpConnectionScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
    )
}
