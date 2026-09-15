// Repeater 屏的状态持有者。

package xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.RepeaterRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.RepeaterRepository

/**
 * Repeater 请求列表的状态持有者。
 *
 * 仓库只读；执行命令走独立命令通路（plan §43 的 execute），不从这里绕过去（rules.md §5.1）。
 * 下拉刷新是一次真正的重读：重建订阅，盘上最新的那份重新发一遍。
 */
class RepeaterViewModel(
    private val repeaterRepository: RepeaterRepository,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) : ViewModel() {
    private val refreshRequest = MutableStateFlow(REFRESH_GENERATION_INITIAL)

    private val isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val loads: Flow<RepeaterLoad> =
        refreshRequest
            .flatMapLatest { observeSources() }
            .onEach { load -> reportLoad(load) }

    val uiState: StateFlow<RepeaterUserInterfaceState> =
        combine(loads, isRefreshing) { load, refreshing ->
            stateOf(load = load, isRefreshing = refreshing)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
            initialValue = RepeaterUserInterfaceState(),
        )

    fun handleIntent(intent: RepeaterUserInterfaceIntent) {
        when (intent) {
            is RepeaterUserInterfaceIntent.Refresh -> requestRefresh()
        }
    }

    private fun requestRefresh() {
        record(category = TechnicalLogCategory.UserInterface, message = "下拉刷新触发：重新读取 Repeater 投影")
        isRefreshing.value = true
        refreshRequest.update { generation -> generation + 1 }
    }

    private fun observeSources(): Flow<RepeaterLoad> {
        val loaded: Flow<RepeaterLoad> =
            repeaterRepository.observeRepeaterRecords().map { records ->
                RepeaterLoad.Loaded(records = records)
            }
        return loaded.catch { throwable -> emit(RepeaterLoad.Failed(throwable = throwable)) }
    }

    private fun reportLoad(load: RepeaterLoad) {
        isRefreshing.value = false
        when (load) {
            is RepeaterLoad.Loading -> Unit

            is RepeaterLoad.Loaded ->
                record(
                    category = TechnicalLogCategory.UserInterface,
                    message = "Repeater 读取完成",
                    attributes = mapOf("recordCount" to load.records.size.toString()),
                )

            is RepeaterLoad.Failed ->
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "Repeater 读取失败",
                    attributes = mapOf("failure" to load.throwable::class.java.simpleName),
                )
        }
    }

    private fun stateOf(
        load: RepeaterLoad,
        isRefreshing: Boolean,
    ): RepeaterUserInterfaceState =
        when (load) {
            is RepeaterLoad.Loading -> RepeaterUserInterfaceState(isRefreshing = isRefreshing)

            is RepeaterLoad.Loaded ->
                RepeaterUserInterfaceState(
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                    records = load.records,
                )

            is RepeaterLoad.Failed ->
                RepeaterUserInterfaceState(
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                    failureReasonResource = R.string.repeater_error_read_failed,
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

    /** 一次读取的三种结局；界面照它决定画等待、错误还是数据。 */
    private sealed interface RepeaterLoad {
        /** 还没读到第一次结果。 */
        data object Loading : RepeaterLoad

        /** 读到了。 */
        data class Loaded(val records: List<RepeaterRecord>) : RepeaterLoad

        /** 读不动了；异常只用于本地日志摘要。 */
        data class Failed(val throwable: Throwable) : RepeaterLoad
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L

        const val REFRESH_GENERATION_INITIAL = 0L
    }
}
