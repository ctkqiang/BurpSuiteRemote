package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 装配层：把仓库与日志端口交给 ViewModel，把状态与意图处理交给无状态的界面，把跳转交回导航壳。 */
@Composable
fun DashboardRoute(
    dashboardRepository: DashboardRepository,
    historyRepository: HistoryRepository,
    onOpenHistoryRecord: (String) -> Unit,
    modifier: Modifier = Modifier,
    technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    val viewModel: DashboardViewModel =
        viewModel { DashboardViewModel(dashboardRepository, historyRepository, technicalLog) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardUserInterfaceEffect.OpenHistoryRecord -> onOpenHistoryRecord(effect.historyIdentifier)
            }
        }
    }

    DashboardScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
    )
}
