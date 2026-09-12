package xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent

/**
 * 一个纯投影：把事件折进状态。
 *
 * 实现里不得出现 I/O、时钟与随机（rules.md §5.6），否则「同一批事件折出的状态一致」这条保证立刻失效。
 */
fun interface ProjectionReducer<State> {
    /**
     * 折一次。
     *
     * 同一事件重复折入不得改变状态：网络只保证 at-least-once，重复投递是常态而不是异常（rules.md §5.5）。
     */
    fun reduce(
        state: State,
        event: RecordedEvent,
    ): State
}
