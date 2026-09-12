package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import kotlinx.serialization.Serializable

/**
 * /v1/status 与 /v1/snapshot 的载荷形状；插件侧两处调用同一个构造函数，因此共用一个类型。
 *
 * @property protocolVersion 插件使用的协议版本。
 * @property remotePort 插件监听端口。
 * @property connectedDeviceCount 当前仍有活动连接的设备数。
 * @property pairedDeviceCount 已配对的设备数。
 * @property latestEventSequenceNumber 插件事件日志的最新序号；快照到达时续传基准推到它。
 */
@Serializable
data class RemoteRuntimeStatePayload(
    val protocolVersion: Int,
    val remotePort: Int,
    val connectedDeviceCount: Int,
    val pairedDeviceCount: Int,
    val latestEventSequenceNumber: Long,
)
