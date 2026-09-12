package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 续传基准来源。
 *
 * 客户端每次握手都要回答「我已经收到哪一号」，答案只能来自本地日志的进度（plan §9）；
 * 由谁记账不是客户端的自由，所以这里是一个注入的端口。
 */
fun interface ResumptionSequenceNumberProvider {
    /** 已收到（或已由快照覆盖）的最大序号。 */
    suspend fun currentSequenceNumber(): Long
}
