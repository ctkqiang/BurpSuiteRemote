// 一条技术日志的内容。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

/**
 * 一条技术日志。
 *
 * 取值一律走 [attributes] 而不是拼进 [message]：拼进消息的取值既没法筛也没法统一脱敏，
 * 而 [attributes] 的键会被实现方逐个检查（rules.md §12）。
 *
 * @property category 日志分类，回答「这是哪一段的事实」。
 * @property message 中文短句；只描述发生了什么，不放取值。
 * @property severity 严重级别，回答「出事时该先看谁」；默认 [TechnicalLogSeverity.Information]。
 *   调用方按事实给：正常完成给 Information，偏离预期但没失败给 Warning，真失败给 Error。
 *   带 [failure] 不等于 Error——「扫到一张不是配对票据的二维码」也带异常，但那是一次正常结果。
 * @property attributes 结构化取值，键用英文小写；敏感键的取值会被实现方抹掉。
 * @property failure 异常摘要的来源；只在真的是故障时给。
 */
data class TechnicalLogEvent(
    val category: TechnicalLogCategory,
    val message: String,
    val severity: TechnicalLogSeverity = TechnicalLogSeverity.Information,
    val attributes: Map<String, String> = emptyMap(),
    val failure: Throwable? = null,
)
