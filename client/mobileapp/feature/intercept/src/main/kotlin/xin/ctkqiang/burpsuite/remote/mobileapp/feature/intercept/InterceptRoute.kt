package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository

/** 装配层：把仓库交给 ViewModel，把状态与意图处理交给无状态的界面。 */
@Composable
fun InterceptRoute(
    interceptRepository: InterceptRepository,
    onOpenInterceptRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: InterceptViewModel = viewModel { InterceptViewModel(interceptRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InterceptUserInterfaceEffect.OpenInterceptRecord ->
                    onOpenInterceptRecord(effect.interceptIdentifier)
            }
        }
    }

    InterceptScreen(uiState = uiState, onIntent = viewModel::handleIntent, modifier = modifier)
}
