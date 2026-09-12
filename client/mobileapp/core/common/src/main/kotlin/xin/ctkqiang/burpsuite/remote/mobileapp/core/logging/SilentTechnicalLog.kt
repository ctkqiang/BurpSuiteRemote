// 什么都不做的技术日志。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

/** 什么都不记的实现；给测试与不需要日志的调用方当默认值用。 */
object SilentTechnicalLog : TechnicalLog {
    override fun record(event: TechnicalLogEvent) = Unit
}
