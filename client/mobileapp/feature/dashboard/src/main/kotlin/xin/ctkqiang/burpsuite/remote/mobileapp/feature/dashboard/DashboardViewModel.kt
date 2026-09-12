package xin.ctkqiang.burpsuite.remote.mobileapp.feature.dashboard

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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.DashboardSummary
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import java.time.Instant

/**
 * 主面板的状态持有者。仓库只读，界面改不了任何事实。
 *
 * 下拉刷新在这里落成一次真正的重读：刷新请求会让订阅重新建立，投影本身是热流，重读到的就是盘上
 * 最新的那份；读到第一次结果（或失败）之前，[DashboardUserInterfaceState.isRefreshing] 一直是 true，
 * 所以指示器不会提前消失，也不会永远转下去。
 */
class DashboardViewModel(
    private val dashboardRepository: DashboardRepository,
    private val historyRepository: HistoryRepository,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
    private val timeProvider: TimeProvider = TimeProvider { Instant.now() },
) : ViewModel() {
    private val effectChannel = Channel<DashboardUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<DashboardUserInterfaceEffect> = effectChannel.receiveAsFlow()

    // 每加一就重建一次订阅，这就是「重新读一遍」的全部含义。
    private val refreshRequest = MutableStateFlow(REFRESH_GENERATION_INITIAL)

    private val isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val loads: Flow<DashboardLoad> =
        refreshRequest
            .flatMapLatest { observeSources() }
            .onEach { load -> reportLoad(load) }

    val uiState: StateFlow<DashboardUserInterfaceState> =
        combine(loads, isRefreshing) { load, refreshing ->
            stateOf(load = load, isRefreshing = refreshing)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
            initialValue = DashboardUserInterfaceState(),
        )

    fun handleIntent(intent: DashboardUserInterfaceIntent) {
        when (intent) {
            is DashboardUserInterfaceIntent.Refresh -> requestRefresh()

            is DashboardUserInterfaceIntent.OpenPairingScanner -> {
                record(category = TechnicalLogCategory.Navigation, message = "从主面板内容区进入扫码配对")
                effectChannel.trySend(DashboardUserInterfaceEffect.OpenPairingScanner)
            }

            is DashboardUserInterfaceIntent.OpenLiveHistory -> {
                record(category = TechnicalLogCategory.Navigation, message = "进入实时历史")
                effectChannel.trySend(DashboardUserInterfaceEffect.OpenLiveHistory)
            }

            is DashboardUserInterfaceIntent.OpenLiveIntercept -> {
                record(category = TechnicalLogCategory.Navigation, message = "进入拦截队列")
                effectChannel.trySend(DashboardUserInterfaceEffect.OpenLiveIntercept)
            }

            is DashboardUserInterfaceIntent.OpenLiveRepeater -> {
                record(category = TechnicalLogCategory.Navigation, message = "进入重放")
                effectChannel.trySend(DashboardUserInterfaceEffect.OpenLiveRepeater)
            }

            is DashboardUserInterfaceIntent.OpenHistoryRecord -> {
                record(category = TechnicalLogCategory.Navigation, message = "打开最近请求的详情")
                effectChannel.trySend(
                    DashboardUserInterfaceEffect.OpenHistoryRecord(intent.historyIdentifier),
                )
            }
        }
    }

    private fun requestRefresh() {
        record(category = TechnicalLogCategory.UserInterface, message = "下拉刷新触发：重新读取主面板汇总与最近请求")
        isRefreshing.value = true
        refreshRequest.update { generation -> generation + 1 }
    }

    // 两个投影都要重新读：汇总与最近列表来自不同的投影，只重读一个会出现对不上的两半。
    private fun observeSources(): Flow<DashboardLoad> {
        val loaded: Flow<DashboardLoad> =
            combine(
                dashboardRepository.observeDashboardSummary(),
                historyRepository.observeHistoryRecords(),
            ) { summary, historyRecords -> DashboardLoad.Loaded(summary = summary, records = historyRecords) }
        return loaded.catch { throwable -> emit(DashboardLoad.Failed(throwable = throwable)) }
    }

    // 日志只写计数与异常摘要：请求体、Cookie、Authorization 与令牌一律不进日志（rules.md §12）。
    private fun reportLoad(load: DashboardLoad) {
        isRefreshing.value = false
        when (load) {
            is DashboardLoad.Loading -> Unit

            is DashboardLoad.Loaded ->
                record(
                    category = TechnicalLogCategory.UserInterface,
                    message = "主面板读取完成",
                    attributes =
                        mapOf(
                            "liveRequestCount" to load.summary.liveRequestCount.toString(),
                            "interceptedCount" to load.summary.interceptedCount.toString(),
                            "savedCount" to load.summary.savedCount.toString(),
                            "recentRecordCount" to load.records.size.toString(),
                        ),
                )

            is DashboardLoad.Failed ->
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "主面板读取失败",
                    attributes = mapOf("failure" to load.throwable::class.java.simpleName),
                )
        }
    }

    private fun stateOf(
        load: DashboardLoad,
        isRefreshing: Boolean,
    ): DashboardUserInterfaceState {
        val now = timeProvider.now()
        return when (load) {
            is DashboardLoad.Loading -> DashboardUserInterfaceState(now = now, isRefreshing = isRefreshing)

            is DashboardLoad.Loaded ->
                DashboardUserInterfaceState(
                    connectionState = load.summary.connectionState,
                    targetHost = load.summary.targetHost,
                    liveRequestCount = load.summary.liveRequestCount,
                    interceptedCount = load.summary.interceptedCount,
                    savedCount = load.summary.savedCount,
                    // 投影按事件序号升序发（HistoryRepository 的约定），取末尾再倒过来就是最近的几条。
                    recentRecords = load.records.takeLast(RECENT_RECORD_COUNT).reversed(),
                    now = now,
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                )

            is DashboardLoad.Failed ->
                DashboardUserInterfaceState(
                    now = now,
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                    failureReasonResource = R.string.dashboard_error_read_failed,
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
    private sealed interface DashboardLoad {
        /** 还没读到第一次结果。 */
        data object Loading : DashboardLoad

        /** 读到了。 */
        data class Loaded(
            val summary: DashboardSummary,
            val records: List<HistoryRecord>,
        ) : DashboardLoad

        /** 读不动了；异常只用于本地日志摘要，不往界面上抛栈。 */
        data class Failed(val throwable: Throwable) : DashboardLoad
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L

        // Recent 只是概览，再多就该翻页了，那是 History 屏的事。
        const val RECENT_RECORD_COUNT = 5

        const val REFRESH_GENERATION_INITIAL = 0L
    }
}
