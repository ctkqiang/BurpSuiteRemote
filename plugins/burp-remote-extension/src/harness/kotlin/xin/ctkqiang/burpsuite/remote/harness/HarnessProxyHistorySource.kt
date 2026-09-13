// 夹具用的代理历史来源：由夹具主动追加条目。

package xin.ctkqiang.burpsuite.remote.harness

import xin.ctkqiang.burpsuite.remote.adapter.BurpProxyHistoryEntry
import xin.ctkqiang.burpsuite.remote.adapter.BurpProxyHistorySource
import java.util.concurrent.CopyOnWriteArrayList

/** 夹具使用的代理历史来源；[appendEntry] 模拟 Burp 新增一条真实记录，读到的顺序就是追加顺序。 */
class HarnessProxyHistorySource : BurpProxyHistorySource {
    private val entries = CopyOnWriteArrayList<BurpProxyHistoryEntry>()

    /** 追加一条条目并原样返回，供调用方记录它的字段。 */
    fun appendEntry(entry: BurpProxyHistoryEntry): BurpProxyHistoryEntry {
        entries.add(entry)
        return entry
    }

    override fun readHistoryEntries(): List<BurpProxyHistoryEntry> = entries.toList()
}
