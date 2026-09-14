/**
 * 作用域适配测试：主机地址由 Burp 当下的字段拼出，标识定位不到时既不写作用域也不返回地址。
 * 全程只用薄接口的测试替身，不构造任何 Montoya 实现类（rules.md §13）。
 */

package xin.ctkqiang.burpsuite.remote.adapter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.HistoryIdentifier

class BurpScopeAdapterTest {
    @Test
    fun `a known identifier writes the scheme and host of that entry into the scope`() {
        val scopeWriter = StubScopeWriter()
        val entry = StubProxyHistoryEntry()
        val scopeAdapter = createAdapter(scopeWriter, entry)

        val writtenHostUrlText = scopeAdapter.includeHistoryHostInScope(identifierOf(entry))

        assertEquals("https://${StubProxyHistoryEntry.DEFAULT_HOST}", writtenHostUrlText)
        assertEquals(listOf("https://${StubProxyHistoryEntry.DEFAULT_HOST}"), scopeWriter.includedHostTexts)
    }

    @Test
    fun `only the host goes into the scope so every path under it is covered`() {
        val scopeWriter = StubScopeWriter()
        val entry = StubProxyHistoryEntry(path = "/api/messages/list")
        val scopeAdapter = createAdapter(scopeWriter, entry)

        scopeAdapter.includeHistoryHostInScope(identifierOf(entry))

        val writtenHostUrlText = scopeWriter.includedHostTexts.single()
        assertEquals("https://${StubProxyHistoryEntry.DEFAULT_HOST}", writtenHostUrlText)
        assertFalse(writtenHostUrlText.contains(entry.path))
    }

    @Test
    fun `an entry that is not secure is written with the plain http scheme`() {
        val scopeWriter = StubScopeWriter()
        val entry = StubProxyHistoryEntry(isSecure = false)
        val scopeAdapter = createAdapter(scopeWriter, entry)

        val writtenHostUrlText = scopeAdapter.includeHistoryHostInScope(identifierOf(entry))

        assertEquals("http://${StubProxyHistoryEntry.DEFAULT_HOST}", writtenHostUrlText)
    }

    @Test
    fun `an identifier that burp never produced writes nothing into the scope`() {
        val scopeWriter = StubScopeWriter()
        val scopeAdapter = createAdapter(scopeWriter, StubProxyHistoryEntry())

        val writtenHostUrlText = scopeAdapter.includeHistoryHostInScope(UNKNOWN_HISTORY_IDENTIFIER)

        assertNull(writtenHostUrlText)
        assertTrue(scopeWriter.includedHostTexts.isEmpty())
    }

    private fun createAdapter(
        scopeWriter: StubScopeWriter,
        vararg entries: BurpProxyHistoryEntry,
    ): BurpScopeAdapter =
        BurpScopeAdapter(
            historyAdapter = BurpHistoryAdapter(StubProxyHistorySource(entries.toMutableList())),
            scopeWriter = scopeWriter,
        )

    private fun identifierOf(entry: BurpProxyHistoryEntry): HistoryIdentifier =
        BurpHistoryAdapter(StubProxyHistorySource(mutableListOf(entry))).toHistoryIdentifier(entry)

    private companion object {
        private val UNKNOWN_HISTORY_IDENTIFIER = HistoryIdentifier("history_ffffffffffffffff")
    }
}
