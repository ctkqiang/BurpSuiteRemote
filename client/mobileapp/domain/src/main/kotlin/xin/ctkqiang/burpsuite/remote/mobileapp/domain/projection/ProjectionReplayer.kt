package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent

/**
 * 投影重放：从初始状态开始，按序号把事件重新折一遍。
 *
 * 与增量更新共用同一个 reducer，所以「重建结果与增量结果一致」才有依据（rules.md §5.6、§5.7）。
 */
class ProjectionReplayer<State>(
    private val projectionReducer: ProjectionReducer<State>,
    private val initialState: State,
) {
    /** 重放；入参顺序不影响结果，这里按序号排定，免得重放结论取决于调用方的遍历顺序。 */
    fun replay(events: List<RecordedEvent>): State =
        events
            .sortedBy { event -> event.sequenceNumber }
            .fold(initialState) { state, event -> projectionReducer.reduce(state, event) }
}
