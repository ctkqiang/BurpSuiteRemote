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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteHistoryScopeWriter
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteRepeaterWriter

/**
 * 装配层：把标识、投影仓库、本体读取端口、作用域写入端口与日志端口交给 ViewModel，
 * 把状态与意图处理交给无状态的界面。
 *
 * 两个远端端口都可能为空（例如预览或裁剪过的装配）。空着时详情屏照实说明这条通路没接上：
 * 本体那一侧摆一句「这版客户端不提供」，作用域那一侧把按钮停用并写明原因——都不假装做过了
 * （rules.md §5.1）。
 */
@Composable
fun HistoryDetailRoute(
    historyIdentifier: String,
    historyRepository: HistoryRepository,
    onOpenSharing: (String) -> Unit,
    modifier: Modifier = Modifier,
    remoteHistoryMessageReader: RemoteHistoryMessageReader? = null,
    remoteHistoryScopeWriter: RemoteHistoryScopeWriter? = null,
    remoteRepeaterWriter: RemoteRepeaterWriter? = null,
    technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    val viewModel: HistoryDetailViewModel =
        viewModel(key = historyIdentifier) {
            HistoryDetailViewModel(
                historyIdentifier = historyIdentifier,
                historyRepository = historyRepository,
                remoteHistoryMessageReader = remoteHistoryMessageReader,
                remoteHistoryScopeWriter = remoteHistoryScopeWriter,
                remoteRepeaterWriter = remoteRepeaterWriter,
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
