package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteConnectionConfiguration
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteResult
import xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote.RemoteRuntimeState

/** 状态快照的来源；续传区间失效时要靠它把基线推到插件当前状态（plan §10）。 */
fun interface RemoteSnapshotSource {
    /** 取一份插件当前状态快照，对应 GET /v1/snapshot。 */
    suspend fun requestSnapshot(configuration: RemoteConnectionConfiguration): RemoteResult<RemoteRuntimeState>
}
