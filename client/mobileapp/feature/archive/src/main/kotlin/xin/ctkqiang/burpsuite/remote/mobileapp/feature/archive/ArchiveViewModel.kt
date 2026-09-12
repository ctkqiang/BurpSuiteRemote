package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 归档屏的状态持有者。归档是只读语义：这一屏没有任何写路径（plan §22）。 */
class ArchiveViewModel(historyRepository: HistoryRepository) : ViewModel() {
    private val effectChannel = Channel<ArchiveUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<ArchiveUserInterfaceEffect> = effectChannel.receiveAsFlow()

    private val selectedTab = MutableStateFlow(ArchiveTab.SavedHistory)

    val uiState: StateFlow<ArchiveUserInterfaceState> =
        combine(historyRepository.observeHistoryRecords(), selectedTab) { historyRecords, currentTab ->
            ArchiveUserInterfaceState(
                selectedTab = currentTab,
                savedRecords = historyRecords.filter { record -> record.isArchived() },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
            initialValue = ArchiveUserInterfaceState(),
        )

    fun handleIntent(intent: ArchiveUserInterfaceIntent) {
        when (intent) {
            is ArchiveUserInterfaceIntent.SelectTab -> selectedTab.update { intent.tab }

            is ArchiveUserInterfaceIntent.OpenSavedRecord ->
                effectChannel.trySend(ArchiveUserInterfaceEffect.OpenHistoryRecord(intent.historyIdentifier))

            is ArchiveUserInterfaceIntent.ShareSavedRecord ->
                effectChannel.trySend(ArchiveUserInterfaceEffect.OpenSharing(intent.historyIdentifier))

            is ArchiveUserInterfaceIntent.OpenScreenshots ->
                effectChannel.trySend(ArchiveUserInterfaceEffect.OpenScreenshots)
        }
    }

    private fun HistoryRecord.isArchived(): Boolean = archiveState == HistoryArchiveState.Archived

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
