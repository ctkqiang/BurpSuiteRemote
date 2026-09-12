package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.SilentTechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLog
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogCategory
import xin.ctkqiang.burpsuite.remote.mobileapp.core.logging.TechnicalLogEvent
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.ProjectionRebuilder
import xin.ctkqiang.burpsuite.remote.mobileapp.data.projection.ProjectionStore
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteRuntimeState
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.ResynchronisationRecorder

/**
 * 重新同步：插件已不保留所需序号区间时的唯一出路（plan §10）。
 *
 * 插件侧 /v1/snapshot 的载荷与 /v1/status 同形，只有运行态、没有投影内容（按服务端代码确认）：
 * 因此「换掉投影」退化为把本地投影按本地日志重建一次，保证它与新的续传基准对齐。
 * 缺口区间内的事件插件已不再提供，这一段事实只能放弃——这一点如实记录，不假装补齐。
 */
class RemoteResynchronisation(
    private val snapshotSource: RemoteSnapshotSource,
    private val resynchronisationRecorder: ResynchronisationRecorder,
    private val projectionRebuilder: ProjectionRebuilder,
    private val projectionStores: List<ProjectionStore>,
    private val technicalLog: TechnicalLog = SilentTechnicalLog,
) {
    /** 取快照、推续传基准、重建本地投影；快照取不到时什么都不改，如实返回失败。 */
    suspend fun resynchronise(configuration: RemoteConnectionConfiguration): RemoteResult<RemoteRuntimeState> =
        when (val snapshotResult = snapshotSource.requestSnapshot(configuration)) {
            is RemoteResult.Failed -> {
                technicalLog.record(
                    TechnicalLogEvent(
                        category = TechnicalLogCategory.Failure,
                        message = "取快照失败，本次重新同步作废",
                        attributes = mapOf("failure" to snapshotResult.failure.toString()),
                    ),
                )
                snapshotResult
            }

            is RemoteResult.Succeeded -> {
                technicalLog.record(
                    TechnicalLogEvent(
                        category = TechnicalLogCategory.EventStream,
                        message = "取到快照，续传基准推进到插件最新序号",
                        attributes =
                            mapOf(
                                "latestEventSequenceNumber" to
                                    snapshotResult.value.latestEventSequenceNumber.toString(),
                            ),
                    ),
                )
                resynchronisationRecorder.recordSnapshot(snapshotResult.value.latestEventSequenceNumber)
                projectionStores.forEach { projectionStore -> projectionRebuilder.rebuild(projectionStore) }
                snapshotResult
            }
        }
}
