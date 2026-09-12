// 拦截队列相关的事实。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.protocol.event

/** 拦截队列相关的事实；拦截项的生命周期由放行或丢弃结束。 */
sealed interface InterceptEvent : DomainEvent
