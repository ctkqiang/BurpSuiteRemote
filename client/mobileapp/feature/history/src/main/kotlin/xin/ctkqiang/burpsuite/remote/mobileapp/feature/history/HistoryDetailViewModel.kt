package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogSeverity
import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteHistoryMessage
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteHistoryMessageReader

/**
 * 历史详情的状态持有者。
 *
 * 两条数据路各自独立：元数据来自本机投影，订阅即得；报文本体要向插件按标识取回，所以它在
 * 进入这一屏时发起一次，失败后由用户按重试再发起一次。之所以不把本体也塞进投影仓库，
 * 是因为仓库读的是本机已有的事实，而本体本来就还没到本机（plan §20）。
 */
class HistoryDetailViewModel(
    private val historyIdentifier: String,
    historyRepository: HistoryRepository,
    private val remoteHistoryMessageReader: RemoteHistoryMessageReader? = null,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) : ViewModel() {
    private val effectChannel = Channel<HistoryDetailUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<HistoryDetailUserInterfaceEffect> = effectChannel.receiveAsFlow()

    private val messageReadState: MutableStateFlow<MessageReadState> = MutableStateFlow(MessageReadState())

    val uiState: StateFlow<HistoryDetailUserInterfaceState> =
        combine(
            historyRepository
                .observeHistoryRecord(HistoryIdentifier(value = historyIdentifier))
                .onEach { record -> reportRecord(observedRecord = record) },
            messageReadState,
        ) { record, messageRead -> mergeState(observedRecord = record, observedMessageRead = messageRead) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
                initialValue = HistoryDetailUserInterfaceState(),
            )

    init {
        loadMessage()
    }

    fun handleIntent(intent: HistoryDetailUserInterfaceIntent) {
        when (intent) {
            is HistoryDetailUserInterfaceIntent.ShareHistoryRecord -> {
                record(category = TechnicalLogCategory.Navigation, message = "用户要求分享或导出这条记录")
                effectChannel.trySend(HistoryDetailUserInterfaceEffect.OpenSharing(historyIdentifier))
            }

            is HistoryDetailUserInterfaceIntent.ReloadHistoryMessage -> loadMessage()
        }
    }

    /**
     * 取一次报文本体。
     *
     * 先把上一轮结果清空再发请求，这样重试时界面会回到「正在取」而不是继续显示上一次的失败，
     * 免得用户以为按钮没反应。
     */
    private fun loadMessage() {
        messageReadState.value = MessageReadState()
        viewModelScope.launch {
            val reader = remoteHistoryMessageReader
            if (reader == null) {
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "客户端没有接上本体读取端口，无法取回报文本体",
                )
                messageReadState.value =
                    MessageReadState(failure = HistoryMessageReadFailure.NotSupported)
                return@launch
            }

            messageReadState.value =
                when (val result = reader.readHistoryMessage(historyIdentifier)) {
                    is RemoteResult.Succeeded -> {
                        record(
                            category = TechnicalLogCategory.UserInterface,
                            message = "报文本体读取完成",
                            attributes =
                                mapOf(
                                    "requestBodyLength" to (result.value.requestBody?.length ?: 0).toString(),
                                    "responseBodyLength" to (result.value.responseBody?.length ?: 0).toString(),
                                ),
                        )
                        MessageReadState(message = result.value)
                    }

                    is RemoteResult.Failed -> {
                        record(
                            category = TechnicalLogCategory.Failure,
                            message = "报文本体读取失败",
                            severity = TechnicalLogSeverity.Warning,
                            attributes = mapOf("failure" to result.failure.toString()),
                        )
                        MessageReadState(failure = HistoryMessageReadFailure.of(result.failure))
                    }
                }
        }
    }

    private fun mergeState(
        observedRecord: HistoryRecord?,
        observedMessageRead: MessageReadState,
    ): HistoryDetailUserInterfaceState =
        HistoryDetailUserInterfaceState(
            record = observedRecord,
            hasLoaded = true,
            message = observedMessageRead.message,
            messageFailure = observedMessageRead.failure,
        )

    private fun reportRecord(observedRecord: HistoryRecord?) {
        record(
            category = TechnicalLogCategory.UserInterface,
            message = if (observedRecord == null) "这条记录已不在本机投影里" else "详情读取完成",
            attributes =
                if (observedRecord == null) {
                    mapOf("historyIdentifier" to historyIdentifier)
                } else {
                    mapOf("annotationCount" to observedRecord.annotationCount.toString())
                },
        )
    }

    private fun record(
        category: TechnicalLogCategory,
        message: String,
        severity: TechnicalLogSeverity = TechnicalLogSeverity.Information,
        attributes: Map<String, String> = emptyMap(),
    ) {
        technicalLog.record(
            TechnicalLogEvent(
                category = category,
                message = message,
                severity = severity,
                attributes = attributes,
            ),
        )
    }

    /** 报文本体那一次读取的结果；两个字段同时为空就是还在取，界面据此画骨架行。 */
    private data class MessageReadState(
        val message: RemoteHistoryMessage? = null,
        val failure: HistoryMessageReadFailure? = null,
    )

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L
    }
}
