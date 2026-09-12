package xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/**
 * 装配层：把仓库、写盘实现与日志端口交给 ViewModel，把状态与意图处理交给无状态的界面。
 *
 * 导出位置由系统文件选择器决定，因此「打开选择器」这件事只能在这一层做（它是 Compose 的东西），
 * ViewModel 只发效果、只接结果（rules.md §8.1）。
 */
@Composable
fun SharingRoute(
    historyIdentifier: String,
    historyRepository: HistoryRepository,
    modifier: Modifier = Modifier,
    technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    val context = LocalContext.current
    val exportNoticeText = stringResource(R.string.sharing_export_notice)
    val viewModel: SharingViewModel =
        viewModel(key = historyIdentifier) {
            SharingViewModel(
                historyIdentifier = historyIdentifier,
                historyRepository = historyRepository,
                // 拿应用上下文：写盘实现的生命周期比界面长，拿着 Activity 会漏。
                exportDestinationWriter =
                    ContentResolverExportDestinationWriter(context.applicationContext),
                exportNoticeText = exportNoticeText,
                technicalLog = technicalLog,
            )
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val destinationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE)) { destinationUri ->
            if (destinationUri != null) {
                viewModel.handleIntent(SharingUserInterfaceIntent.WriteExport(destinationUri.toString()))
            }
        }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SharingUserInterfaceEffect.RequestExportDestination ->
                    destinationLauncher.launch(effect.suggestedFileName)
            }
        }
    }

    SharingScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
    )
}

// 导出包是 ZIP；.burpremote 只是它面向用户的扩展名。
private const val EXPORT_MIME_TYPE = "application/zip"
