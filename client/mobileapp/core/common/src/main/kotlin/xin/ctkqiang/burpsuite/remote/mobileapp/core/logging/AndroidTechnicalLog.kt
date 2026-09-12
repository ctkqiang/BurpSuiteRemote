// 技术日志落到 logcat 的实现。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

import android.util.Log

/**
 * 落到 logcat 的技术日志。
 *
 * 用 verbose 级别：这些是排查用的细节，不是产品行为；级别调高会把 logcat 刷满，
 * 真出故障时反而找不到那一行。敏感键的取值在落地前抹成 `***`。
 */
class AndroidTechnicalLog(
    private val tag: String = DEFAULT_LOG_TAG,
) : TechnicalLog {
    override fun record(event: TechnicalLogEvent) {
        Log.v(tag, render(event))
    }

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
        // 与各屏统一的 tag，方便 adb logcat -s 一起过滤出本应用的日志。
        const val DEFAULT_LOG_TAG = "BurpRemote"

        const val REDACTED_VALUE = "***"

        const val FAILURE_FRAME_LIMIT = 5
    }
}
