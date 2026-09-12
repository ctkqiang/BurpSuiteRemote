package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository

/** 装配层：把主面板汇总交给 ViewModel，把状态交给无状态的界面。 */
@Composable
fun RepeaterRoute(
    dashboardRepository: DashboardRepository,
    modifier: Modifier = Modifier,
) {
    val viewModel: RepeaterViewModel = viewModel { RepeaterViewModel(dashboardRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RepeaterScreen(uiState = uiState, modifier = modifier)
}
