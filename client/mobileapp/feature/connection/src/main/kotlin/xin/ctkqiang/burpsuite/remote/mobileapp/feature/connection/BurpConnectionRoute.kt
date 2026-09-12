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
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteControlClient

/**
 * 装配层：把连接状态与远程客户端交给 ViewModel，把状态与意图处理交给无状态的界面。
 *
 * 权限弹窗只能在这一层发起（`rememberLauncherForActivityResult` 是 Compose 的东西），
 * 因此 ViewModel 只发「要申请了」这个效果，实际弹窗由这里执行，结果再作为意图回给 ViewModel。
 */
@Composable
fun BurpConnectionRoute(
    connectionState: Flow<ConnectionState>,
    remoteControlClient: RemoteControlClient?,
    modifier: Modifier = Modifier,
) {
    val viewModel: BurpConnectionViewModel =
        viewModel { BurpConnectionViewModel(connectionState, remoteControlClient) }
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

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BurpConnectionUserInterfaceEffect.LaunchCameraPermissionRequest ->
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    BurpConnectionScreen(uiState = uiState, onIntent = viewModel::handleIntent, modifier = modifier)
}
