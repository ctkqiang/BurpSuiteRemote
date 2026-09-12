package xin.ctkqiang.burpsuite.remote.mobileapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.ConnectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.DashboardSummary
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryArchiveState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.InterceptState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.DashboardRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.HistoryRepository
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.repository.InterceptRepository

/**
 * 主面板汇总。
 *
 * 它没有自己的事实来源（domain 的 `DashboardSummary` 就是这么声明的）：计数来自两个投影，
 * 连接状态来自远程会话，因此这里只做一路合并，不落任何表。
 */
class ProjectionDashboardRepository(
    private val historyRepository: HistoryRepository,
    private val interceptRepository: InterceptRepository,
    private val connectionState: Flow<ConnectionState>,
) : DashboardRepository {
    override fun observeDashboardSummary(): Flow<DashboardSummary> =
        combine(
            historyRepository.observeHistoryRecords(),
            interceptRepository.observeInterceptRecords(),
            connectionState,
        ) { historyRecords, interceptRecords, currentConnectionState ->
            DashboardSummary(
                connectionState = currentConnectionState,
                // 历史列表按事件序号升序（HistoryRepository 的约定），最后一条就是最近的一条。
                targetHost = historyRecords.lastOrNull()?.host,
                liveRequestCount = historyRecords.count { record -> record.archiveState == HistoryArchiveState.Live },
                interceptedCount =
                    interceptRecords.count { record ->
                        record.state == InterceptState.Pending || record.state == InterceptState.Modified
                    },
                savedCount = historyRecords.count { record -> record.archiveState == HistoryArchiveState.Archived },
            )
        }
}
