// Repeater 相关的事实。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.event

/** Repeater 相关的事实；一次请求会依次产生创建、开始执行、执行完成三条事实。 */
sealed interface RepeaterEvent : DomainEvent
