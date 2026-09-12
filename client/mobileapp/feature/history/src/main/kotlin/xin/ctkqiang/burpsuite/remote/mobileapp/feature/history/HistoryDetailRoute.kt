package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 装配层：把标识与仓库交给 ViewModel，把状态与意图处理交给无状态的界面。 */
@Composable
fun HistoryDetailRoute(
    historyIdentifier: String,
    historyRepository: HistoryRepository,
    onOpenSharing: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HistoryDetailViewModel =
        viewModel(key = historyIdentifier) { HistoryDetailViewModel(historyIdentifier, historyRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryDetailUserInterfaceEffect.OpenSharing -> onOpenSharing(effect.historyIdentifier)
            }
        }
    }

    HistoryDetailScreen(uiState = uiState, onIntent = viewModel::handleIntent, modifier = modifier)
}
