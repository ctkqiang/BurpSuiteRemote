package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 装配层：把仓库交给 ViewModel，把状态交给无状态的界面，把跳转交回导航壳。 */
@Composable
fun HistoryRoute(
    historyRepository: HistoryRepository,
    onOpenHistoryRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HistoryViewModel = viewModel { HistoryViewModel(historyRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(uiState = uiState, onOpenHistoryRecord = onOpenHistoryRecord, modifier = modifier)
}
