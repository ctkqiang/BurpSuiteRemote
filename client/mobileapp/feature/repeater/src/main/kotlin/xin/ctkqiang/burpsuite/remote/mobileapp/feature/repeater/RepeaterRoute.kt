// Repeater 屏的装配。

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.RepeaterRepository

/** 装配层：把 Repeater 读端口与日志交给 ViewModel，把状态与意图处理交给无状态的界面。 */
@Composable
fun RepeaterRoute(
    repeaterRepository: RepeaterRepository,
    modifier: Modifier = Modifier,
    technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    val viewModel: RepeaterViewModel =
        viewModel { RepeaterViewModel(repeaterRepository, technicalLog) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RepeaterScreen(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        modifier = modifier,
    )
}
