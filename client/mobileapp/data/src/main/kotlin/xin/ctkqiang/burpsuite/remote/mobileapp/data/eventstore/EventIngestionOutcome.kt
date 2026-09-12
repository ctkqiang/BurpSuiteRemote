package xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore

/** 一条事件入账的结局。 */
sealed interface EventIngestionOutcome {
    /** 事件已落盘并折入投影。 */
    data object Applied : EventIngestionOutcome

    /** 已经收到过；at-least-once 下的常态，丢掉即可，不是错误。 */
    data object AlreadyDelivered : EventIngestionOutcome

    /**
     * 序号断洞，这条事件先不落盘，必须从 [resumeAfterSequenceNumber] 续传补齐缺失区间。
     *
     * 静默继续会让投影永久漏掉一段事实，而漏掉的事实不会自己补回来（rules.md §5.5、plan §63）。
     */
    data class ResynchronisationRequired(val resumeAfterSequenceNumber: Long) : EventIngestionOutcome

    /** 某个序号被另一条事件复用：不是重复投递，而是协议不一致，必须拒收。 */
    data class SequenceNumberReused(val sequenceNumber: Long) : EventIngestionOutcome
}
