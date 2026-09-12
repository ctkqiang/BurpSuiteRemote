// HTTP 历史记录相关的事实。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.event

/** HTTP 历史记录相关的事实；历史记录的内容归 Burp 所有，客户端只投影这里的事实。 */
sealed interface HistoryEvent : DomainEvent
