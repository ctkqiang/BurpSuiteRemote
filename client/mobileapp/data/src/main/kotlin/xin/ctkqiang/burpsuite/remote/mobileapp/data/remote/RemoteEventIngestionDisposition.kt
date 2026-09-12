package xin.ctkqiang.burpsuite.remote.mobileapp.data.remote

import xin.ctkqiang.burpsuite.remote.mobileapp.data.eventstore.EventIngestionOutcome

/** 一条事件的入账结论该把会话导向何种结局。 */
internal object RemoteEventIngestionDisposition {
    /**
     * 断洞必须带着新基准重连接续传，绝不静默继续（rules.md §5.5）；没有结局时返回 null，继续收。
     */
    fun sessionEndFor(outcome: EventIngestionOutcome): RemoteSessionEnd? =
        when (outcome) {
            EventIngestionOutcome.Applied,
            EventIngestionOutcome.AlreadyDelivered,
            -> null

            is EventIngestionOutcome.ResynchronisationRequired -> RemoteSessionEnd.ResumeRequired

            is EventIngestionOutcome.SequenceNumberReused -> RemoteSessionEnd.ProtocolMismatch
        }
}
