// 技术日志落到 logcat 的实现。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

import android.util.Log

/**
 * 落到 logcat 的技术日志。
 *
 * 落地级别跟着 [TechnicalLogEvent.severity] 走，而不是一律 verbose：级别是这条日志唯一
 * 能被 logcat 侧直接过滤的维度，压成同一档等于把刚建好的那根轴在这里掐断。
 * 排查完的 Debug 细节不该继续占用 logcat 的默认视图，而 Warning 与 Error 必须默认可见。
 *
 * 敏感键的取值在落地前抹成 `***`（rules.md §12）。
 *
 * @param tag logcat 的标签；与各屏统一，方便 `adb logcat -s` 一起过滤出本应用的日志。
 */
class AndroidTechnicalLog(
    private val tag: String = DEFAULT_LOG_TAG,
) : TechnicalLog {
    /** 按级别选一个 logcat 落地级别，再按统一格式渲染正文。 */
    override fun record(event: TechnicalLogEvent) {
        val rendered = render(event)
        when (event.severity) {
            TechnicalLogSeverity.Debug -> Log.d(tag, rendered)
            TechnicalLogSeverity.Information -> Log.i(tag, rendered)
            TechnicalLogSeverity.Warning -> Log.w(tag, rendered)
            TechnicalLogSeverity.Error -> Log.e(tag, rendered)
        }
    }

    /** 把一条日志渲染成 `[分类] 消息 键=值 … 原因: 摘要`；分类与级别分工不同，正文只出分类。 */
    private fun render(event: TechnicalLogEvent): String {
        val builder = StringBuilder()
        builder.append('[').append(event.category.displayName).append("] ").append(event.message)
        for ((attributeName, attributeValue) in event.attributes) {
            builder.append(' ').append(attributeName).append('=')
            builder.append(
                if (SensitiveAttributeKeys.isSensitive(attributeName)) REDACTED_VALUE else attributeValue,
            )
        }
        event.failure?.let { failure -> builder.append(" 原因: ").append(summarise(failure)) }
        return builder.toString()
    }

    // 摘要只到调用链的头几层：完整栈是给调试器看的，写进日志只剩噪声。
    private fun summarise(failure: Throwable): String {
        val stackSummary =
            failure.stackTrace
                .take(FAILURE_FRAME_LIMIT)
                .joinToString(separator = " -> ") { frame ->
                    frame.className.substringAfterLast('.') + "." + frame.methodName
                }
        return failure.javaClass.simpleName + ": " + failure.message + " @" + stackSummary
    }

    private val TechnicalLogCategory.displayName: String
        get() =
            when (this) {
                TechnicalLogCategory.Pairing -> "配对"
                TechnicalLogCategory.Transport -> "传输"
                TechnicalLogCategory.EventStream -> "事件通道"
                TechnicalLogCategory.EventIngestion -> "事件摄入"
                TechnicalLogCategory.Navigation -> "导航"
                TechnicalLogCategory.UserInterface -> "界面"
                TechnicalLogCategory.Failure -> "故障"
            }

    private companion object {
        const val DEFAULT_LOG_TAG = "BurpRemote"

        const val REDACTED_VALUE = "***"

        const val FAILURE_FRAME_LIMIT = 5
    }
}
