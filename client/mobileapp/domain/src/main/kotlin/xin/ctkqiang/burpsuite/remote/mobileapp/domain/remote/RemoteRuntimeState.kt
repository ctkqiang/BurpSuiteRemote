package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 插件运行态的只读快照。
 *
 * /v1/status 与 /v1/snapshot 返回同一个形状（插件侧 RemoteHttpServer 里两处调用同一个构造函数），
 * 因此用一个类型承载，免得出现两个字段完全相同的类型。
 */
data class RemoteRuntimeState(
    /** 插件使用的协议版本。 */
    val protocolVersion: Int,
    /** 插件监听端口。 */
    val remotePort: Int,
    /** 当前连着的设备数。 */
    val connectedDeviceCount: Int,
    /** 已配对的设备数。 */
    val pairedDeviceCount: Int,
    /** 插件事件日志的最新序号；续传基准就是它。 */
    val latestEventSequenceNumber: Long,
)
