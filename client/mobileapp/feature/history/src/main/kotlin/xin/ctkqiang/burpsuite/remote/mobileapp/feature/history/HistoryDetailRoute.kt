package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteHistoryMessageReader

/**
 * 装配层：把标识、投影仓库、本体读取端口与日志端口交给 ViewModel，把状态与意图处理交给无状态的界面。
 *
 * 本体读取端口可能为空（例如预览或裁剪过的装配），此时详情屏照实说明这条通路没接上，
 * 而不是假装取过了（rules.md §5.1）。
 */
@Composable
fun HistoryDetailRoute(
    historyIdentifier: String,
    historyRepository: HistoryRepository,
    onOpenSharing: (String) -> Unit,
    modifier: Modifier = Modifier,
    remoteHistoryMessageReader: RemoteHistoryMessageReader? = null,
    technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    val viewModel: HistoryDetailViewModel =
        viewModel(key = historyIdentifier) {
            HistoryDetailViewModel(
                historyIdentifier = historyIdentifier,
                historyRepository = historyRepository,
                remoteHistoryMessageReader = remoteHistoryMessageReader,
                technicalLog = technicalLog,
            )
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryDetailUserInterfaceEffect.OpenSharing -> onOpenSharing(effect.historyIdentifier)
            }
        }
    }

    HistoryDetailScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
    )
}
