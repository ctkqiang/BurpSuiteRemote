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
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteHistoryScopeWriter
import xin.ctkqiang.burpsuite.remote.mobileapp.ui.navigation.RemoteRepeaterWriter

/**
 * 历史详情的状态持有者。
 *
 * 三条数据路各自独立：元数据来自本机投影，订阅即得；报文本体要向插件按标识取回，所以它在
 * 进入这一屏时发起一次，失败后由用户按重试再发起一次；加入作用域是一条写命令，只在用户按下
 * 按钮时发一次。之所以不把本体也塞进投影仓库，是因为仓库读的是本机已有的事实，而本体本来
 * 就还没到本机（plan §20）。
 *
 * 写入端口为空是装配缺口，不是插件缺陷：此时按钮会自报
 * [HistoryScopeWriteConclusion.WriterUnavailable] 并停用——说清「这一版客户端没接上」，
 * 比让用户去升级一个本来就够新的插件有用。
 */
class HistoryDetailViewModel(
    private val historyIdentifier: String,
    historyRepository: HistoryRepository,
    private val remoteHistoryMessageReader: RemoteHistoryMessageReader? = null,
    private val remoteHistoryScopeWriter: RemoteHistoryScopeWriter? = null,
    private val remoteRepeaterWriter: RemoteRepeaterWriter? = null,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) : ViewModel() {
    private val effectChannel = Channel<HistoryDetailUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<HistoryDetailUserInterfaceEffect> = effectChannel.receiveAsFlow()

    private val messageReadState: MutableStateFlow<MessageReadState> = MutableStateFlow(MessageReadState())

    /**
     * 最近一次「加入作用域」的结论；为空就是还没点过。
     *
     * 只放一个可空的结果值，而不是「正在写」「写完了」两个布尔量：这一屏不给这条写命令准备骨架态，
     * 一次本机 REST 写入短到来不及画任何中间态，多出来的布尔量只会多一处需要跟着同步的真相。
     */
    private val scopeWriteConclusionState: MutableStateFlow<HistoryScopeWriteConclusion?> =
        MutableStateFlow(null)

    /**
     * 最近一次「送往重放」的结论；为空就是还没点过。
     *
     * 与 [scopeWriteConclusionState] 同一套写法：一次本机 REST 写入短到来不及画任何中间态，
     * 多出来的「正在写」布尔量只会多一处需要跟着同步的真相。
     */
    private val repeaterConclusionState: MutableStateFlow<HistoryRepeaterConclusion?> =
        MutableStateFlow(null)

    val uiState: StateFlow<HistoryDetailUserInterfaceState> =
        combine(
            historyRepository
                .observeHistoryRecord(HistoryIdentifier(value = historyIdentifier))
                .onEach { record -> reportRecord(observedRecord = record) },
            messageReadState,
            scopeWriteConclusionState,
            repeaterConclusionState,
        ) { record, messageRead, scopeWriteConclusion, repeaterConclusion ->
            mergeState(
                observedRecord = record,
                observedMessageRead = messageRead,
                observedScopeConclusion = scopeWriteConclusion,
                isScopeWriteAvailable = remoteHistoryScopeWriter != null,
                observedRepeaterConclusion = repeaterConclusion,
                isRepeaterWriteAvailable = remoteRepeaterWriter != null,
            )
        }
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

            is HistoryDetailUserInterfaceIntent.AddHistoryHostToScope -> addHostToScope()

            is HistoryDetailUserInterfaceIntent.SendToRepeater -> sendToRepeater()
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

    /**
     * 把这条记录的主机写进 Burp 作用域。
     *
     * 不像取报文本体那样先把上一轮结果清空：这一屏没有为这条写命令准备骨架态，清空只会让刚才那句
     * 结论闪一下白，而一次本机 REST 写入短到来不及画任何中间态。用户重复点也写不坏——插件的写入
     * 本身是幂等的（rules.md §5.5）。
     */
    private fun addHostToScope() {
        viewModelScope.launch {
            val writer = remoteHistoryScopeWriter
            if (writer == null) {
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "客户端没有接上作用域写入端口，无法把主机加入作用域",
                )
                scopeWriteConclusionState.value = HistoryScopeWriteConclusion.WriterUnavailable
                return@launch
            }

            scopeWriteConclusionState.value =
                when (val result = writer.addHistoryHostToScope(historyIdentifier)) {
                    is RemoteResult.Succeeded -> {
                        record(
                            category = TechnicalLogCategory.UserInterface,
                            message = "主机已加入作用域",
                            attributes = mapOf("historyIdentifier" to historyIdentifier),
                        )
                        HistoryScopeWriteConclusion.Added
                    }

                    is RemoteResult.Failed -> {
                        record(
                            category = TechnicalLogCategory.Failure,
                            message = "主机加入作用域失败",
                            severity = TechnicalLogSeverity.Warning,
                            attributes = mapOf("failure" to result.failure.toString()),
                        )
                        HistoryScopeWriteConclusion.of(result.failure)
                    }
                }
        }
    }

    /**
     * 把这条记录的原始请求推送到 Repeater。
     *
     * 与 [addHostToScope] 同理：不先清空上一轮结论——一次本机 REST 写入短到来不及画中间态，
     * 清空只会让上次的结论闪一下白。推送 Repeater 需要 requestText，因此先检查本体是否已取回；
     * 本体尚未取回时结论是 [HistoryRepeaterConclusion.MessageNotLoaded]，下一步是先取回本体或重试。
     */
    private fun sendToRepeater() {
        viewModelScope.launch {
            val writer = remoteRepeaterWriter
            if (writer == null) {
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "客户端没有接上 Repeater 写入端口，无法推送到 Repeater",
                )
                repeaterConclusionState.value = HistoryRepeaterConclusion.WriterUnavailable
                return@launch
            }

            val message = messageReadState.value.message
            val requestHeaders = message?.requestHeaders
            if (requestHeaders == null) {
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "报文本体尚未取回，无法拼出 requestText",
                    severity = TechnicalLogSeverity.Warning,
                )
                repeaterConclusionState.value = HistoryRepeaterConclusion.MessageNotLoaded
                return@launch
            }

            val requestBody = message.requestBody
            val requestText = buildString {
                append(requestHeaders)
                if (!requestBody.isNullOrBlank()) {
                    if (!requestHeaders.endsWith("\n")) {
                        append("\r\n")
                    }
                    append("\r\n")
                    append(requestBody)
                }
            }

            repeaterConclusionState.value =
                when (val result = writer.sendToRepeater(requestText, tabName = REPEATER_TAB_NAME)) {
                    is RemoteResult.Succeeded -> {
                        record(
                            category = TechnicalLogCategory.UserInterface,
                            message = "请求已推送到 Repeater",
                            attributes = mapOf("historyIdentifier" to historyIdentifier),
                        )
                        HistoryRepeaterConclusion.Sent
                    }

                    is RemoteResult.Failed -> {
                        record(
                            category = TechnicalLogCategory.Failure,
                            message = "推送到 Repeater 失败",
                            severity = TechnicalLogSeverity.Warning,
                            attributes = mapOf("failure" to result.failure.toString()),
                        )
                        HistoryRepeaterConclusion.of(result.failure)
                    }
                }
        }
    }

    private fun mergeState(
        observedRecord: HistoryRecord?,
        observedMessageRead: MessageReadState,
        observedScopeConclusion: HistoryScopeWriteConclusion?,
        isScopeWriteAvailable: Boolean,
        observedRepeaterConclusion: HistoryRepeaterConclusion?,
        isRepeaterWriteAvailable: Boolean,
    ): HistoryDetailUserInterfaceState =
        HistoryDetailUserInterfaceState(
            record = observedRecord,
            hasLoaded = true,
            message = observedMessageRead.message,
            messageFailure = observedMessageRead.failure,
            scopeConclusion = observedScopeConclusion,
            isScopeWriteAvailable = isScopeWriteAvailable,
            repeaterConclusion = observedRepeaterConclusion,
            isRepeaterWriteAvailable = isRepeaterWriteAvailable,
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

        // 推送到 Repeater 时给 tab 起的名字；不绑记录标识，因为 Repeater tab 名字是给人看的。
        const val REPEATER_TAB_NAME = "Mobile"
    }
}
