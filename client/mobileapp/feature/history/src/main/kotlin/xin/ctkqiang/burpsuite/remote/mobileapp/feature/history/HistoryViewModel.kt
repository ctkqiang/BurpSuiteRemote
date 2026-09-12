package xin.ctkqiang.burpsuite.remote.mobileapp.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import java.time.Instant

/**
 * 实时历史的状态持有者。仓库只读，界面没有任何改造事实的入口。
 *
 * 这一屏只显示仍在实时投影里的记录：已归档的副本有自己的语义与自己的页签（plan §22），
 * 两类混在一张表里，用户没法判断哪一条还会被后续事件改写。
 *
 * 下拉刷新是一次真正的重读：重建订阅后，盘上最新的那份重新发一遍。
 */
class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
    private val timeProvider: TimeProvider = TimeProvider { Instant.now() },
) : ViewModel() {
    private val effectChannel = Channel<HistoryUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<HistoryUserInterfaceEffect> = effectChannel.receiveAsFlow()

    private val refreshRequest = MutableStateFlow(REFRESH_GENERATION_INITIAL)

    private val isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val loads: Flow<HistoryLoad> =
        refreshRequest
            .flatMapLatest { observeSources() }
            .onEach { load -> reportLoad(load) }

    val uiState: StateFlow<HistoryUserInterfaceState> =
        combine(loads, isRefreshing) { load, refreshing ->
            stateOf(load = load, isRefreshing = refreshing)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
            initialValue = HistoryUserInterfaceState(),
        )

    fun handleIntent(intent: HistoryUserInterfaceIntent) {
        when (intent) {
            is HistoryUserInterfaceIntent.Refresh -> requestRefresh()

            is HistoryUserInterfaceIntent.OpenHistoryRecord -> {
                record(
                    category = TechnicalLogCategory.Navigation,
                    message = "打开历史详情",
                    attributes = mapOf("historyIdentifier" to intent.historyIdentifier),
                )
                effectChannel.trySend(HistoryUserInterfaceEffect.OpenHistoryRecord(intent.historyIdentifier))
            }
        }
    }

    private fun requestRefresh() {
        record(category = TechnicalLogCategory.UserInterface, message = "下拉刷新触发：重新读取历史投影")
        isRefreshing.value = true
        refreshRequest.update { generation -> generation + 1 }
    }

    private fun observeSources(): Flow<HistoryLoad> {
        val loaded: Flow<HistoryLoad> =
            historyRepository.observeHistoryRecords().map { historyRecords ->
                HistoryLoad.Loaded(
                    records = historyRecords.filter { record -> record.archiveState == HistoryArchiveState.Live },
                )
            }
        return loaded.catch { throwable -> emit(HistoryLoad.Failed(throwable = throwable)) }
    }

    private fun reportLoad(load: HistoryLoad) {
        isRefreshing.value = false
        when (load) {
            is HistoryLoad.Loading -> Unit

            is HistoryLoad.Loaded ->
                record(
                    category = TechnicalLogCategory.UserInterface,
                    message = "历史读取完成",
                    attributes = mapOf("liveRecordCount" to load.records.size.toString()),
                )

            is HistoryLoad.Failed ->
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "历史读取失败",
                    attributes = mapOf("failure" to load.throwable::class.java.simpleName),
                )
        }
    }

    private fun stateOf(
        load: HistoryLoad,
        isRefreshing: Boolean,
    ): HistoryUserInterfaceState {
        val now = timeProvider.now()
        return when (load) {
            is HistoryLoad.Loading -> HistoryUserInterfaceState(now = now, isRefreshing = isRefreshing)

            is HistoryLoad.Loaded ->
                HistoryUserInterfaceState(
                    records = load.records,
                    now = now,
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                )

            is HistoryLoad.Failed ->
                HistoryUserInterfaceState(
                    now = now,
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                    failureReasonResource = R.string.history_error_read_failed,
                )
        }
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

    /** 一次读取的三种结局；界面照它决定画等待、错误还是数据。 */
    private sealed interface HistoryLoad {
        /** 还没读到第一次结果。 */
        data object Loading : HistoryLoad

        /** 读到了。 */
        data class Loaded(val records: List<HistoryRecord>) : HistoryLoad

        /** 读不动了；异常只用于本地日志摘要。 */
        data class Failed(val throwable: Throwable) : HistoryLoad
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L

        const val REFRESH_GENERATION_INITIAL = 0L
    }
}
