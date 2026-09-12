// 测试替身：可手动触发的历史增长信号源。

package xin.ctkqiang.burpsuite.remote.adapter

/** 信号源替身：手动触发信号，并记录订阅是否被取消。 */
class StubProxyHistorySignalSource : BurpProxyHistorySignalSource {
    private var recordedSubscription: StubHistorySignalSubscription? = null

    /** 订阅是否已经被取消。 */
    val isSubscriptionCancelled: Boolean
        get() = recordedSubscription?.isCancelled == true

    override fun subscribeToHistorySignals(onHistoryMayHaveGrown: () -> Unit): BurpHistorySignalSubscription {
        val createdSubscription = StubHistorySignalSubscription(onHistoryMayHaveGrown)
        recordedSubscription = createdSubscription
        return createdSubscription
    }

    /** 模拟 Burp 刚处理完一次代理交互；取消订阅后再触发等同于回调残留。 */
    fun fireSignal() {
        recordedSubscription?.fireSignal()
    }
}

private class StubHistorySignalSubscription(
    private val onHistoryMayHaveGrown: () -> Unit,
) : BurpHistorySignalSubscription {
    var isCancelled: Boolean = false
        private set

    override fun cancel() {
        isCancelled = true
    }

    fun fireSignal() {
        onHistoryMayHaveGrown()
    }
}
