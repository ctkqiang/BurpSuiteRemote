// 一次历史信号订阅的句柄。

package xin.ctkqiang.burpsuite.remote.adapter

/** 一次历史信号订阅的句柄；重复取消是安全的。 */
interface BurpHistorySignalSubscription {
    /** 取消订阅，把注册到 Burp 里的回调摘掉。 */
    fun cancel()
}
