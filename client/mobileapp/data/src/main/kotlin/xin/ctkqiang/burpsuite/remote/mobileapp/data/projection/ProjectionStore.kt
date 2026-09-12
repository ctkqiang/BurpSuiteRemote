package xin.ctkqiang.burpsuite.remote.mobileapp.data.projection

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.event.RecordedEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.projection.ProjectionName

/**
 * 一个投影的写入口。
 *
 * 投影是派生数据：写它不会改变事实，与日志冲突时以日志为准并整份重建（rules.md §5.6）。
 * 领域层只声明了读模型与校验点的端口，写入口留在这里，因为只有适配层知道投影落在哪张表。
 */
interface ProjectionStore {
    /** 本投影的名字；校验点按它分开记账。 */
    val projectionName: ProjectionName

    /** 这条事件是否属于本投影。 */
    fun handles(event: RecordedEvent): Boolean

    /** 折入一条事件；由调用方保证它处在事件落盘的那个事务里。 */
    suspend fun apply(event: RecordedEvent)

    /** 丢掉现有内容，按给定事件整份重建；重建结果必须与增量构建一致（rules.md §13）。 */
    suspend fun replaceAll(events: List<RecordedEvent>)
}
