package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 一份远端载荷原文。
 *
 * 历史类端点依赖尚未实现的 Burp 适配层，插件只回「尚未实现」，载荷字段集因此还没定下来：
 * 与其替它编一个字段集，不如先原样承载文本（rules.md §11）。
 *
 * @property encodedText 载荷的原始 JSON 文本。
 */
@JvmInline
value class RemotePayload(val encodedText: String)
