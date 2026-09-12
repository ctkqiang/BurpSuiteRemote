// 测试替身：可增删条目的 Burp 代理历史来源。

package xin.ctkqiang.burpsuite.remote.adapter

/** 可增删条目的历史来源替身；读取次数用来断言卸载之后没有再碰 Burp。 */
class StubProxyHistorySource(
    private val entries: MutableList<BurpProxyHistoryEntry> = mutableListOf(),
) : BurpProxyHistorySource {
    var readCount: Int = 0
        private set

    override fun readHistoryEntries(): List<BurpProxyHistoryEntry> {
        readCount++
        return entries.toList()
    }

    /** 追加一条 Burp 刚刚产生的条目。 */
    fun appendEntry(entry: BurpProxyHistoryEntry) {
        entries.add(entry)
    }

    /** 模拟操作者清空了代理历史。 */
    fun clearEntries() {
        entries.clear()
    }
}
