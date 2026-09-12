// 一条技术日志的内容。

package xin.ctkqiang.burpsuite.remote.mobileapp.core.logging

/**
 * 一条技术日志。
 *
 * 取值一律走 [attributes] 而不是拼进 [message]：拼进消息的取值既没法筛也没法统一脱敏，
 * 而 [attributes] 的键会被实现方逐个检查（rules.md §12）。
 *
 * @property category 日志分类。
 * @property message 中文短句；只描述发生了什么，不放取值。
 * @property attributes 结构化取值，键用英文小写；敏感键的取值会被实现方抹掉。
 * @property failure 异常摘要的来源；只在真的是故障时给。
 */
data class TechnicalLogEvent(
    val category: TechnicalLogCategory,
    val message: String,
    val attributes: Map<String, String> = emptyMap(),
    val failure: Throwable? = null,
)
