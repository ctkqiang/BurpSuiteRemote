package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.InterceptIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository

/**
 * 拦截项详情的状态持有者。
 *
 * 草稿与投影分开存：投影是事实，用户改的是还没提交的意图，两者混在一起会让「当前到底是什么」
 * 说不清楚（plan §2.1 与 §2.3 分属两方状态）。
 */
class InterceptDetailViewModel(
    interceptIdentifier: String,
    interceptRepository: InterceptRepository,
) : ViewModel() {
    private val draft = MutableStateFlow(RequestDraft())

    val uiState: StateFlow<InterceptDetailUserInterfaceState> =
        combine(
            interceptRepository.observeInterceptRecord(InterceptIdentifier(value = interceptIdentifier)),
            draft,
        ) { record, currentDraft -> stateOf(record = record, draft = currentDraft) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = InterceptDetailUserInterfaceState(),
            )

    fun handleIntent(intent: InterceptDetailUserInterfaceIntent) {
        when (intent) {
            is InterceptDetailUserInterfaceIntent.UpdateRequestLineInput ->
                draft.update { currentDraft -> currentDraft.copy(requestLineInput = intent.requestLineInput) }

            is InterceptDetailUserInterfaceIntent.UpdateRequestHeadersInput ->
                draft.update { currentDraft -> currentDraft.copy(requestHeadersInput = intent.requestHeadersInput) }
        }
    }

    private fun stateOf(
        record: InterceptRecord?,
        draft: RequestDraft,
    ): InterceptDetailUserInterfaceState =
        InterceptDetailUserInterfaceState(
            record = record,
            hasLoaded = true,
            // 投影里还没有请求行与请求头——报文本体要按标识另取，客户端还没有那条通路。
            requestLineInput = draft.requestLineInput,
            requestHeadersInput = draft.requestHeadersInput,
            // 放行、丢弃、提交修改都要发控制命令，客户端现在没有这条通路。
            canSendCommand = false,
        )

    private data class RequestDraft(
        val requestLineInput: String = "",
        val requestHeadersInput: String = "",
    )

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
