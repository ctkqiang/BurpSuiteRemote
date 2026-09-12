package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 历史详情的状态持有者。正文不在这一屏取：报文本体按标识另行请求，这里只承载元数据（plan §20）。 */
class HistoryDetailViewModel(
    private val historyIdentifier: String,
    historyRepository: HistoryRepository,
) : ViewModel() {
    private val effectChannel = Channel<HistoryDetailUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<HistoryDetailUserInterfaceEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<HistoryDetailUserInterfaceState> =
        historyRepository
            .observeHistoryRecord(HistoryIdentifier(value = historyIdentifier))
            .map { record -> HistoryDetailUserInterfaceState(record = record, hasLoaded = true) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = HistoryDetailUserInterfaceState(),
            )

    fun handleIntent(intent: HistoryDetailUserInterfaceIntent) {
        when (intent) {
            is HistoryDetailUserInterfaceIntent.ShareHistoryRecord ->
                effectChannel.trySend(HistoryDetailUserInterfaceEffect.OpenSharing(historyIdentifier))
        }
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
