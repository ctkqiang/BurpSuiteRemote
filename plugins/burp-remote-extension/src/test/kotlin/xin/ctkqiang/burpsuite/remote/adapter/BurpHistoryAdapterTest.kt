/**
 * 适配器映射测试：协议载荷只含 Burp 给出的元数据，正文只在按标识取回时才出现，身份可复现。
 * 全程只用薄接口的测试替身，不构造任何 Montoya 实现类（rules.md §13）。
 */

package xin.ctkqiang.burpsuite.remote.adapter

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xin.ctkqiang.burpsuite.remote.protocol.HistoryIdentifier

class BurpHistoryAdapterTest {
    @Test
    fun `the history list payload carries the burp metadata of every entry`() {
        val adapter = createAdapter(StubProxyHistoryEntry())

        val historyItems = historyItemsOf(adapter)

        assertEquals(1, historyItems.size)
        val item = historyItems[0]
        assertEquals(StubProxyHistoryEntry.DEFAULT_METHOD, item.textAt("method"))
        assertEquals(StubProxyHistoryEntry.DEFAULT_HOST, item.textAt("host"))
        assertEquals(StubProxyHistoryEntry.DEFAULT_PATH, item.textAt("path"))
        assertEquals(StubProxyHistoryEntry.DEFAULT_STATUS_CODE.toString(), item.textAt("status"))
        assertEquals(adapter.toHistoryIdentifier(StubProxyHistoryEntry()).value, item.textAt("historyIdentifier"))
    }

    @Test
    fun `the metadata payload carries no montoya type name and no message body`() {
        val adapter = createAdapter(StubProxyHistoryEntry())

        val metadataText = historyItemsOf(adapter)[0].toString().lowercase()

        assertFalse(metadataText.contains("montoya"))
        assertFalse(metadataText.contains("proxyhttprequestresponse"))
        assertFalse(metadataText.contains("httprequestresponse"))
        assertFalse(metadataText.contains(StubProxyHistoryEntry.DEFAULT_REQUEST_BODY_TEXT.lowercase()))
        assertFalse(metadataText.contains(StubProxyHistoryEntry.DEFAULT_RESPONSE_BODY_TEXT.lowercase()))
    }

    @Test
    fun `the metadata payload contains nothing but the documented fields`() {
        val adapter = createAdapter(StubProxyHistoryEntry())

        assertEquals(
            setOf("historyIdentifier", "method", "host", "path", "status"),
            historyItemsOf(adapter)[0].jsonObject.keys,
        )
    }

    @Test
    fun `the message payload carries the full request and response text`() {
        val stubEntry = StubProxyHistoryEntry()
        val adapter = createAdapter(stubEntry)

        val message = adapter.buildHistoryMessagePayload(adapter.toHistoryIdentifier(stubEntry))

        assertEquals(StubProxyHistoryEntry.DEFAULT_REQUEST_HEADERS_TEXT, message.textAt("requestHeaders"))
        assertEquals(StubProxyHistoryEntry.DEFAULT_REQUEST_BODY_TEXT, message.textAt("requestBody"))
        assertEquals(StubProxyHistoryEntry.DEFAULT_RESPONSE_HEADERS_TEXT, message.textAt("responseHeaders"))
        assertEquals(StubProxyHistoryEntry.DEFAULT_RESPONSE_BODY_TEXT, message.textAt("responseBody"))
        assertEquals(StubProxyHistoryEntry.DEFAULT_PATH, message.textAt("path"))
    }

    @Test
    fun `a message whose response has not arrived yet reports no response text`() {
        val stubEntry = StubProxyHistoryEntry(hasResponse = false, responseHeadersText = null, responseBodyText = null)
        val adapter = createAdapter(stubEntry)

        val message = adapter.buildHistoryMessagePayload(adapter.toHistoryIdentifier(stubEntry))

        assertEquals(JsonNull, message?.jsonObject?.get("responseHeaders"))
        assertEquals(JsonNull, message?.jsonObject?.get("responseBody"))
    }

    @Test
    fun `an identifier that burp never produced yields no payload`() {
        val adapter = createAdapter(StubProxyHistoryEntry())

        assertNull(adapter.buildHistoryMessagePayload(HistoryIdentifier("history_0000000000000000")))
    }

    @Test
    fun `the same entry always maps to the same identifier while a different entry does not`() {
        val adapter = createAdapter(StubProxyHistoryEntry(), StubProxyHistoryEntry(path = "/api/other"))

        val firstIdentifier = adapter.toHistoryIdentifier(StubProxyHistoryEntry())
        val repeatedIdentifier = adapter.toHistoryIdentifier(StubProxyHistoryEntry())
        val otherIdentifier = adapter.toHistoryIdentifier(StubProxyHistoryEntry(path = "/api/other"))

        assertEquals(firstIdentifier, repeatedIdentifier)
        assertNotEquals(firstIdentifier, otherIdentifier)
        assertTrue(firstIdentifier.value.startsWith("history_"))
    }

    private fun createAdapter(vararg entries: BurpProxyHistoryEntry): BurpHistoryAdapter =
        BurpHistoryAdapter(StubProxyHistorySource(entries.toMutableList()))

    private fun historyItemsOf(adapter: BurpHistoryAdapter): JsonArray =
        requireNotNull(adapter.buildHistoryListPayload().jsonObject["historyItems"]?.jsonArray)

    private fun JsonElement?.textAt(fieldName: String): String? =
        this?.jsonObject?.get(fieldName)?.jsonPrimitive?.content
}
