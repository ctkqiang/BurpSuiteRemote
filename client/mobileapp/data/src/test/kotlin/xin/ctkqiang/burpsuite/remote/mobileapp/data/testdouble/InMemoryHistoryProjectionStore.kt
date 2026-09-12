package xin.ctkqiang.burpsuite.remote.mobileapp.data.testdouble

import xin.ctkqiang.burpsuite.remote.mobileapp.core.model.HistoryIdentifier
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.ProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.model.HistoryRecord
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.HistoryProjectionReducer
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.HistoryProjectionState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionReplayer

// 内存历史投影：用真实 reducer 与重放器，因此重建结果能和增量结果直接比。
internal class InMemoryHistoryProjectionStore : ProjectionStore {
    private var recordsByIdentifier: Map<HistoryIdentifier, HistoryRecord> = emptyMap()

    // 注入失败，用来验证「事件与校验点要么都成、要么都不成」。
    var applyFailure: Throwable? = null

    override val projectionName: ProjectionName = ProjectionName.HISTORY

    override fun handles(event: RecordedEvent): Boolean =
        HistoryProjectionReducer.affectedHistoryIdentifier(event) != null

    override suspend fun apply(event: RecordedEvent) {
        applyFailure?.let { failure -> throw failure }

        val state = HistoryProjectionState(recordsByIdentifier = recordsByIdentifier)
        recordsByIdentifier = HistoryProjectionReducer.reduce(state, event).recordsByIdentifier
    }

    override suspend fun replaceAll(events: List<RecordedEvent>) {
        recordsByIdentifier =
            ProjectionReplayer(HistoryProjectionReducer, HistoryProjectionState()).replay(events).recordsByIdentifier
    }

    // 供断言读。
    fun currentRecords(): Map<HistoryIdentifier, HistoryRecord> = recordsByIdentifier
}
