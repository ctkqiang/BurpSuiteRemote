package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository

/** 拦截队列的状态持有者。仓库只读，放行与丢弃是命令，不从这里绕过去（rules.md §5.1）。 */
class InterceptViewModel(interceptRepository: InterceptRepository) : ViewModel() {
    private val effectChannel = Channel<InterceptUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<InterceptUserInterfaceEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<InterceptUserInterfaceState> =
        interceptRepository
            .observeInterceptRecords()
            .map { interceptRecords -> InterceptUserInterfaceState(records = interceptRecords) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = InterceptUserInterfaceState(),
            )

    fun handleIntent(intent: InterceptUserInterfaceIntent) {
        when (intent) {
            is InterceptUserInterfaceIntent.OpenInterceptRecord ->
                effectChannel.trySend(
                    InterceptUserInterfaceEffect.OpenInterceptRecord(intent.interceptIdentifier),
                )
        }
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
