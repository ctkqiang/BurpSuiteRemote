package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 实时历史的状态持有者。仓库只读，界面没有任何改造事实的入口。 */
class HistoryViewModel(historyRepository: HistoryRepository) : ViewModel() {
    val uiState: StateFlow<HistoryUserInterfaceState> =
        historyRepository
            .observeHistoryRecords()
            .map { historyRecords -> HistoryUserInterfaceState(records = historyRecords) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = HistoryUserInterfaceState(),
            )

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
