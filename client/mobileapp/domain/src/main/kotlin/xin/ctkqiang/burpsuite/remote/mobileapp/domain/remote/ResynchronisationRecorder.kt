package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 快照到达后的记账端口。
 *
 * 插件已不保留所需序号区间时，取到快照只完成了一半：本地续传基准必须一起推到快照那一刻，
 * 否则下一次重连还会拿着过期的序号去要一段已经不存在的区间（plan §10）。
 */
fun interface ResynchronisationRecorder {
    /** 把本地续传基准推到快照对应的最新序号。 */
    suspend fun recordSnapshot(snapshotSequenceNumber: Long)
}
