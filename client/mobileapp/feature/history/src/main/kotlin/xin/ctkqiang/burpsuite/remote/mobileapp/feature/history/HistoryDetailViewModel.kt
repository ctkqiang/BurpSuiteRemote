package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository

/** 历史详情的状态持有者。正文不在这一屏取：报文本体按标识另行请求，这里只承载元数据（plan §20）。 */
class HistoryDetailViewModel(
    private val historyIdentifier: String,
    historyRepository: HistoryRepository,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) : ViewModel() {
    private val effectChannel = Channel<HistoryDetailUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<HistoryDetailUserInterfaceEffect> = effectChannel.receiveAsFlow()

    val uiState: StateFlow<HistoryDetailUserInterfaceState> =
        historyRepository
            .observeHistoryRecord(HistoryIdentifier(value = historyIdentifier))
            .onEach { record -> reportRecord(observedRecord = record) }
            .map { record -> HistoryDetailUserInterfaceState(record = record, hasLoaded = true) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = HistoryDetailUserInterfaceState(),
            )

    fun handleIntent(intent: HistoryDetailUserInterfaceIntent) {
        when (intent) {
            is HistoryDetailUserInterfaceIntent.ShareHistoryRecord -> {
                record(category = TechnicalLogCategory.Navigation, message = "用户要求分享或导出这条记录")
                effectChannel.trySend(HistoryDetailUserInterfaceEffect.OpenSharing(historyIdentifier))
            }
        }
    }

    private fun reportRecord(observedRecord: HistoryRecord?) {
        record(
            category = TechnicalLogCategory.UserInterface,
            message = if (observedRecord == null) "这条记录已不在本机投影里" else "详情读取完成",
            attributes =
                if (observedRecord == null) {
                    mapOf("historyIdentifier" to historyIdentifier)
                } else {
                    mapOf(
                        "archiveState" to observedRecord.archiveState.toString(),
                        "annotationCount" to observedRecord.annotationCount.toString(),
                    )
                },
        )
    }

    private fun record(
        category: TechnicalLogCategory,
        message: String,
        attributes: Map<String, String> = emptyMap(),
    ) {
        technicalLog.record(
            TechnicalLogEvent(category = category, message = message, attributes = attributes),
        )
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
