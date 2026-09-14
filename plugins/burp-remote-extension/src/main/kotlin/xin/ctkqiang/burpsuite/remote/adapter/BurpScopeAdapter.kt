// 按历史条目把主机加入 Burp 作用域。

package xin.ctkqiang.burpsuite.remote.adapter

import xin.ctkqiang.burpsuite.remote.protocol.HistoryIdentifier

/**
 * 按历史标识把条目所属主机加入作用域。
 *
 * 主机地址由 Burp 给出的字段拼出，客户端不必自己拼：客户端手上的记录可能已经过期，
 * 而这里读的是 Burp 当下的事实，两边因此不会分叉。
 */
class BurpScopeAdapter(
    private val historyAdapter: BurpHistoryAdapter,
    private val scopeWriter: BurpScopeWriter,
) {
    /**
     * 定位历史标识对应的条目，并把其主机（含协议方案）加入作用域。
     *
     * @return 实际加入作用域的主机地址；标识在当前历史里找不到时返回 null，调用方据此回一个明确的失败。
     */
    fun includeHistoryHostInScope(historyIdentifier: HistoryIdentifier): String? {
        val entry =
            historyAdapter.readHistoryEntries()
                .firstOrNull { candidateEntry ->
                    historyAdapter.toHistoryIdentifier(candidateEntry) == historyIdentifier
                }
                ?: return null
        val hostUrlText = historyAdapter.schemeTextOf(entry) + HOST_URL_SEPARATOR + entry.host
        scopeWriter.includeHostInScope(hostUrlText)
        return hostUrlText
    }

    private companion object {
        private const val HOST_URL_SEPARATOR = "://"
    }
}
