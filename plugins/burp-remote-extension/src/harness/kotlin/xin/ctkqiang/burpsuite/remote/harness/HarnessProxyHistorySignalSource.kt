// 夹具用的历史增长信号源：不依赖 Burp 的钩子，由夹具自己触发。

package xin.ctkqiang.burpsuite.remote.harness

import xin.ctkqiang.burpsuite.remote.adapter.BurpHistorySignalSubscription
import xin.ctkqiang.burpsuite.remote.adapter.BurpProxyHistorySignalSource
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** 夹具使用的历史增长信号源；真实 Burp 里由钩子通知，这里由夹具主动通知。 */
class HarnessProxyHistorySignalSource : BurpProxyHistorySignalSource {
    private val signalHandler = AtomicReference<(() -> Unit)?>(null)

    override fun subscribeToHistorySignals(onHistoryMayHaveGrown: () -> Unit): BurpHistorySignalSubscription {
        signalHandler.set(onHistoryMayHaveGrown)
        return HarnessHistorySignalSubscription(onCancel = { signalHandler.set(null) })
    }

    /** 代替 Burp 的钩子通知「历史可能刚刚增长」。 */
    fun signalHistoryMayHaveGrown() {
        signalHandler.get()?.invoke()
    }
}

// 取消时把回调摘掉；重复取消是安全的，摘掉之后再通知不会打到已卸载的订阅者。
private class HarnessHistorySignalSubscription(private val onCancel: () -> Unit) : BurpHistorySignalSubscription {
    private val isCancelled = AtomicBoolean(false)

    override fun cancel() {
        if (isCancelled.compareAndSet(false, true)) {
            onCancel()
        }
    }
}
