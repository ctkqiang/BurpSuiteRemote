package xin.ctkqiang.burpsuite.remote.mobileapp.domain.sync

/** 一条到货事件的判定结论。 */
sealed interface EventSynchronisationDecision {
    /** 事件是新的且序号接得上，可以落盘。 */
    data object Accepted : EventSynchronisationDecision

    /** 已经收到过；at-least-once 下的常态，丢掉即可，不是错误。 */
    data object Duplicate : EventSynchronisationDecision

    /**
     * 序号断洞：这条事件先不落盘，必须从 [resumeAfterSequenceNumber] 续传补齐缺失区间。
     *
     * 静默继续会让投影永久漏掉一段事实，而漏掉的事实不会自己补回来（rules.md §5.5、plan §63）。
     */
    data class GapDetected(val resumeAfterSequenceNumber: Long) : EventSynchronisationDecision

    /** 某个序号被另一条事件复用：这不是重复投递，而是协议不一致，必须拒绝。 */
    data class SequenceNumberReused(val sequenceNumber: Long) : EventSynchronisationDecision
}
