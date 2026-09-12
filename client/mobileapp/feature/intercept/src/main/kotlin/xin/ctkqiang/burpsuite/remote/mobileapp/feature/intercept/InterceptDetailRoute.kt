package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository

/** 装配层：把标识与仓库交给 ViewModel，把状态与意图处理交给无状态的界面。 */
@Composable
fun InterceptDetailRoute(
    interceptIdentifier: String,
    interceptRepository: InterceptRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: InterceptDetailViewModel =
        viewModel(key = interceptIdentifier) {
            InterceptDetailViewModel(interceptIdentifier, interceptRepository)
        }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    InterceptDetailScreen(uiState = uiState, onIntent = viewModel::handleIntent, modifier = modifier)
}
