// Burp 侧「代理历史可能刚刚增长」的信号源。

package xin.ctkqiang.burpsuite.remote.adapter

/** 历史增长信号源；信号只表示「去看一眼」，事实永远以 Burp 自己的历史为准。 */
interface BurpProxyHistorySignalSource {
    /** 订阅信号；返回的句柄必须在卸载时取消，否则 Burp 里会残留回调。 */
    fun subscribeToHistorySignals(onHistoryMayHaveGrown: () -> Unit): BurpHistorySignalSubscription
}
