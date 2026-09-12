package xin.ctkqiang.burpsuite.remote.mobileapp.feature.intercept

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
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.time.TimeProvider
import java.time.Instant

/**
 * 拦截队列的状态持有者。仓库只读，放行与丢弃是命令，不从这里绕过去（rules.md §5.1）。
 *
 * 下拉刷新是一次真正的重读：重建订阅，盘上最新的那份重新发一遍。
 */
class InterceptViewModel(
    private val interceptRepository: InterceptRepository,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
    private val timeProvider: TimeProvider = TimeProvider { Instant.now() },
) : ViewModel() {
    private val effectChannel = Channel<InterceptUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<InterceptUserInterfaceEffect> = effectChannel.receiveAsFlow()

    private val refreshRequest = MutableStateFlow(REFRESH_GENERATION_INITIAL)

    private val isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val loads: Flow<InterceptLoad> =
        refreshRequest
            .flatMapLatest { observeSources() }
            .onEach { load -> reportLoad(load) }

    val uiState: StateFlow<InterceptUserInterfaceState> =
        combine(loads, isRefreshing) { load, refreshing ->
            stateOf(load = load, isRefreshing = refreshing)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
            initialValue = InterceptUserInterfaceState(),
        )

    fun handleIntent(intent: InterceptUserInterfaceIntent) {
        when (intent) {
            is InterceptUserInterfaceIntent.Refresh -> requestRefresh()

            is InterceptUserInterfaceIntent.OpenInterceptRecord -> {
                record(
                    category = TechnicalLogCategory.Navigation,
                    message = "打开拦截详情",
                    attributes = mapOf("interceptIdentifier" to intent.interceptIdentifier),
                )
                effectChannel.trySend(InterceptUserInterfaceEffect.OpenInterceptRecord(intent.interceptIdentifier))
            }
        }
    }

    private fun requestRefresh() {
        record(category = TechnicalLogCategory.UserInterface, message = "下拉刷新触发：重新读取拦截投影")
        isRefreshing.value = true
        refreshRequest.update { generation -> generation + 1 }
    }

    private fun observeSources(): Flow<InterceptLoad> {
        val loaded: Flow<InterceptLoad> =
            interceptRepository.observeInterceptRecords().map { interceptRecords ->
                InterceptLoad.Loaded(records = interceptRecords)
            }
        return loaded.catch { throwable -> emit(InterceptLoad.Failed(throwable = throwable)) }
    }

    private fun reportLoad(load: InterceptLoad) {
        isRefreshing.value = false
        when (load) {
            is InterceptLoad.Loading -> Unit

            is InterceptLoad.Loaded -> {
                val pendingCount = load.records.count { record -> record.state.isWaiting }
                record(
                    category = TechnicalLogCategory.UserInterface,
                    message = "拦截读取完成",
                    attributes =
                        mapOf(
                            "recordCount" to load.records.size.toString(),
                            "pendingCount" to pendingCount.toString(),
                        ),
                )
            }

            is InterceptLoad.Failed ->
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "拦截读取失败",
                    attributes = mapOf("failure" to load.throwable::class.java.simpleName),
                )
        }
    }

    private fun stateOf(
        load: InterceptLoad,
        isRefreshing: Boolean,
    ): InterceptUserInterfaceState {
        val now = timeProvider.now()
        return when (load) {
            is InterceptLoad.Loading -> InterceptUserInterfaceState(now = now, isRefreshing = isRefreshing)

            is InterceptLoad.Loaded ->
                InterceptUserInterfaceState(
                    records = load.records,
                    now = now,
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                )

            is InterceptLoad.Failed ->
                InterceptUserInterfaceState(
                    now = now,
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                    failureReasonResource = R.string.intercept_error_read_failed,
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
    private sealed interface InterceptLoad {
        /** 还没读到第一次结果。 */
        data object Loading : InterceptLoad

        /** 读到了。 */
        data class Loaded(val records: List<InterceptRecord>) : InterceptLoad

        /** 读不动了；异常只用于本地日志摘要。 */
        data class Failed(val throwable: Throwable) : InterceptLoad
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L

        const val REFRESH_GENERATION_INITIAL = 0L
    }
}
