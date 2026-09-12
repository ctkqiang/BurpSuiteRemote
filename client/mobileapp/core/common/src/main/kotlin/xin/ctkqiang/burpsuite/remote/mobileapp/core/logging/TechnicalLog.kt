// 技术日志的写入端口。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

/**
 * 技术日志端口。
 *
 * 由构造参数注入而不是全局单例：测试要能断言「记了什么」，也要能整条关掉；
 * 一个够得着全局日志的测试，最后一定会去断言与本次行为无关的输出。
 */
fun interface TechnicalLog {
    /** 记一条日志；实现方决定落到哪里、以什么格式落。 */
    fun record(event: TechnicalLogEvent)
}
