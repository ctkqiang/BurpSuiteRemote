package xin.ctkqiang.burpsuite.remote.mobileapp.domain.remote

/**
 * 一份远端载荷原文。
 *
 * 只剩历史列表端点（GET /v1/history）用它：那张列表的字段集还没定下来，与其替插件编一个形状，
 * 不如先原样承载文本（rules.md §11）。单条报文已经改用 [RemoteHistoryMessage]，因为那条端点
 * 的字段集与插件实现一一对应，客户端照实建模即可。
 *
 * @property encodedText 载荷的原始 JSON 文本。
 */
@JvmInline
value class RemotePayload(val encodedText: String)
