// 读取 Burp 代理历史的端口。

package xin.ctkqiang.burpsuite.remote.adapter

/** Burp 代理历史的读取端口；适配器只依赖它，映射逻辑因此不依赖 Montoya。 */
interface BurpProxyHistorySource {
    /** 按 Burp 自己的排列顺序读出全部代理历史条目。 */
    fun readHistoryEntries(): List<BurpProxyHistoryEntry>
}
