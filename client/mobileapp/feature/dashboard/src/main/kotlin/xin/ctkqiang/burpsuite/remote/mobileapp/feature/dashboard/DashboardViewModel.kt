package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.DashboardSummary
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 主面板的状态持有者。仓库只读，界面改不了任何事实。 */
class DashboardViewModel(
    dashboardRepository: DashboardRepository,
    historyRepository: HistoryRepository,
) : ViewModel() {
    private val effectChannel = Channel<DashboardUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<DashboardUserInterfaceEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<DashboardUserInterfaceState> =
        combine(
            dashboardRepository.observeDashboardSummary(),
            historyRepository.observeHistoryRecords(),
        ) { summary, historyRecords -> stateOf(summary, historyRecords) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = DashboardUserInterfaceState(),
            )

    fun handleIntent(intent: DashboardUserInterfaceIntent) {
        when (intent) {
            is DashboardUserInterfaceIntent.OpenLiveHistory ->
                effectChannel.trySend(DashboardUserInterfaceEffect.OpenLiveHistory)

            is DashboardUserInterfaceIntent.OpenLiveIntercept ->
                effectChannel.trySend(DashboardUserInterfaceEffect.OpenLiveIntercept)

            is DashboardUserInterfaceIntent.OpenLiveRepeater ->
                effectChannel.trySend(DashboardUserInterfaceEffect.OpenLiveRepeater)

            is DashboardUserInterfaceIntent.OpenHistoryRecord ->
                effectChannel.trySend(
                    DashboardUserInterfaceEffect.OpenHistoryRecord(intent.historyIdentifier),
                )
        }
    }

    private fun stateOf(
        summary: DashboardSummary,
        historyRecords: List<HistoryRecord>,
    ): DashboardUserInterfaceState =
        DashboardUserInterfaceState(
            connectionState = summary.connectionState,
            targetHost = summary.targetHost,
            liveRequestCount = summary.liveRequestCount,
            interceptedCount = summary.interceptedCount,
            savedCount = summary.savedCount,
            // 投影按事件序号升序发（HistoryRepository 的约定），取末尾再倒过来就是最近的几条。
            recentRecords = historyRecords.takeLast(RECENT_RECORD_COUNT).reversed(),
        )

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L

        // Recent 只是概览，再多就该翻页了，那是 History 屏的事。
        const val RECENT_RECORD_COUNT = 5
    }
}
