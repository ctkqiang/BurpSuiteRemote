package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository

/** 重放屏的状态持有者。这一屏唯一能读的事实是连接状态，它来自主面板汇总里的同一路来源。 */
class RepeaterViewModel(dashboardRepository: DashboardRepository) : ViewModel() {
    val uiState: StateFlow<RepeaterUserInterfaceState> =
        dashboardRepository
            .observeDashboardSummary()
            .map { summary -> RepeaterUserInterfaceState(connectionState = summary.connectionState) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = RepeaterUserInterfaceState(),
            )

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次状态。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
