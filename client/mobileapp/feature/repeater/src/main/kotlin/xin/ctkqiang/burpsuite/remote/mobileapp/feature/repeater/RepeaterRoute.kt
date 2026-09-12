package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository

/** 装配层：把主面板汇总与日志端口交给 ViewModel，把状态与意图处理交给无状态的界面。 */
@Composable
fun RepeaterRoute(
    dashboardRepository: DashboardRepository,
    modifier: Modifier = Modifier,
    technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    val viewModel: RepeaterViewModel =
        viewModel { RepeaterViewModel(dashboardRepository, technicalLog) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RepeaterScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
    )
}
