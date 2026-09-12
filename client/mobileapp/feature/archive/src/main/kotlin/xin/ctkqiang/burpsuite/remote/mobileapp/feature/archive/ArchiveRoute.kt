package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 装配层：把仓库交给 ViewModel，把状态与意图处理交给无状态的界面，把跳转交回导航壳。 */
@Composable
fun ArchiveRoute(
    historyRepository: HistoryRepository,
    onOpenHistoryRecord: (String) -> Unit,
    onOpenSharing: (String) -> Unit,
    onOpenScreenshots: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ArchiveViewModel = viewModel { ArchiveViewModel(historyRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ArchiveUserInterfaceEffect.OpenHistoryRecord -> onOpenHistoryRecord(effect.historyIdentifier)
                is ArchiveUserInterfaceEffect.OpenSharing -> onOpenSharing(effect.historyIdentifier)
                is ArchiveUserInterfaceEffect.OpenScreenshots -> onOpenScreenshots()
            }
        }
    }

    ArchiveScreen(uiState = uiState, onIntent = viewModel::handleIntent, modifier = modifier)
}
