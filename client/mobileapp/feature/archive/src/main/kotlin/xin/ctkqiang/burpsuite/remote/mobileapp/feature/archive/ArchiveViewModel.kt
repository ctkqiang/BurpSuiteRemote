package xin.ctkqiang.burpsuite.remote.mobileapp.feature.archive

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
 * 归档屏的状态持有者。归档是只读语义：这一屏没有任何写路径（plan §22）。
 *
 * 页签选择与投影分开存：选哪个页签是界面状态，投影是事实，两者混在一起会让「现在到底有什么」
 * 说不清楚。
 */
class ArchiveViewModel(
    private val historyRepository: HistoryRepository,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
    private val timeProvider: TimeProvider = TimeProvider { Instant.now() },
) : ViewModel() {
    private val effectChannel = Channel<ArchiveUserInterfaceEffect>(Channel.BUFFERED)

    /** 一次性效果流；导航这类动作走这里，不塞进状态。 */
    val effects: Flow<ArchiveUserInterfaceEffect> = effectChannel.receiveAsFlow()

    private val selectedTab = MutableStateFlow(ArchiveTab.SavedHistory)

    private val refreshRequest = MutableStateFlow(REFRESH_GENERATION_INITIAL)

    private val isRefreshing = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val loads: Flow<ArchiveLoad> =
        refreshRequest
            .flatMapLatest { observeSources() }
            .onEach { load -> reportLoad(load) }

    val uiState: StateFlow<ArchiveUserInterfaceState> =
        combine(loads, selectedTab, isRefreshing) { load, currentTab, refreshing ->
            stateOf(load = load, selectedTab = currentTab, isRefreshing = refreshing)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLISECONDS),
            initialValue = ArchiveUserInterfaceState(),
        )

    fun handleIntent(intent: ArchiveUserInterfaceIntent) {
        when (intent) {
            is ArchiveUserInterfaceIntent.Refresh -> requestRefresh()

            is ArchiveUserInterfaceIntent.SelectTab -> {
                record(
                    category = TechnicalLogCategory.UserInterface,
                    message = "切换归档页签",
                    attributes = mapOf("tab" to intent.tab.name),
                )
                selectedTab.update { intent.tab }
            }

            is ArchiveUserInterfaceIntent.OpenSavedRecord -> {
                record(category = TechnicalLogCategory.Navigation, message = "打开已保存记录的详情")
                effectChannel.trySend(ArchiveUserInterfaceEffect.OpenHistoryRecord(intent.historyIdentifier))
            }

            is ArchiveUserInterfaceIntent.ShareSavedRecord -> {
                record(category = TechnicalLogCategory.Navigation, message = "分享或导出已保存记录")
                effectChannel.trySend(ArchiveUserInterfaceEffect.OpenSharing(intent.historyIdentifier))
            }

            is ArchiveUserInterfaceIntent.OpenScreenshots -> {
                record(category = TechnicalLogCategory.Navigation, message = "打开截图工作区")
                effectChannel.trySend(ArchiveUserInterfaceEffect.OpenScreenshots)
            }
        }
    }

    private fun requestRefresh() {
        record(category = TechnicalLogCategory.UserInterface, message = "下拉刷新触发：重新读取归档投影")
        isRefreshing.value = true
        refreshRequest.update { generation -> generation + 1 }
    }

    private fun observeSources(): Flow<ArchiveLoad> {
        val loaded: Flow<ArchiveLoad> =
            historyRepository.observeHistoryRecords().map { historyRecords ->
                ArchiveLoad.Loaded(
                    savedRecords =
                        historyRecords.filter { record -> record.archiveState == HistoryArchiveState.Archived },
                )
            }
        return loaded.catch { throwable -> emit(ArchiveLoad.Failed(throwable = throwable)) }
    }

    private fun reportLoad(load: ArchiveLoad) {
        isRefreshing.value = false
        when (load) {
            is ArchiveLoad.Loading -> Unit

            is ArchiveLoad.Loaded ->
                record(
                    category = TechnicalLogCategory.UserInterface,
                    message = "归档读取完成",
                    attributes = mapOf("savedRecordCount" to load.savedRecords.size.toString()),
                )

            is ArchiveLoad.Failed ->
                record(
                    category = TechnicalLogCategory.Failure,
                    message = "归档读取失败",
                    attributes = mapOf("failure" to load.throwable::class.java.simpleName),
                )
        }
    }

    private fun stateOf(
        load: ArchiveLoad,
        selectedTab: ArchiveTab,
        isRefreshing: Boolean,
    ): ArchiveUserInterfaceState {
        val now = timeProvider.now()
        return when (load) {
            is ArchiveLoad.Loading ->
                ArchiveUserInterfaceState(
                    selectedTab = selectedTab,
                    now = now,
                    isRefreshing = isRefreshing,
                )

            is ArchiveLoad.Loaded ->
                ArchiveUserInterfaceState(
                    selectedTab = selectedTab,
                    savedRecords = load.savedRecords,
                    now = now,
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                )

            is ArchiveLoad.Failed ->
                ArchiveUserInterfaceState(
                    selectedTab = selectedTab,
                    now = now,
                    hasLoaded = true,
                    isRefreshing = isRefreshing,
                    failureReasonResource = R.string.archive_error_read_failed,
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
    private sealed interface ArchiveLoad {
        /** 还没读到第一次结果。 */
        data object Loading : ArchiveLoad

        /** 读到了。 */
        data class Loaded(val savedRecords: List<HistoryRecord>) : ArchiveLoad

        /** 读不动了；异常只用于本地日志摘要。 */
        data class Failed(val throwable: Throwable) : ArchiveLoad
    }

    private companion object {
        // 界面转屏或短暂离开时别急着停掉上游，回来时就不用重新读一次盘。
        const val STOP_TIMEOUT_MILLISECONDS = 5_000L

        const val REFRESH_GENERATION_INITIAL = 0L
    }
}
