package xin.ctkqiang.burpsuite.remote.mobileapp.domain.model

/**
 * 主面板一眼要看懂的那几个数（plan §44）。
 *
 * 它是派生值而不是投影：连接状态来自远程会话，其余计数来自各投影，因此没有自己的校验点。
 */
data class DashboardSummary(
    /** 当前连接状态。 */
    val connectionState: ConnectionState,
    /** 最近一条历史记录的目标主机；还没有任何记录时为 null。 */
    val targetHost: String?,
    /** 仍在实时投影中的请求数。 */
    val liveRequestCount: Int,
    /** 还停在拦截队列里的请求数。 */
    val interceptedCount: Int,
    /** 已归档的请求数。 */
    val savedCount: Int,
)
