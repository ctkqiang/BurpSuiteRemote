package xin.ctkqiang.burpsuite.remote.mobileapp.data.burpfile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Base64

class BurpItemsXmlCodecTest {
    @Test
    fun `an encoded document carries the structure burp imports`() {
        val encoded = BurpItemsXmlCodec.encode(items = listOf(ITEM), exportedAt = EXPORTED_AT)

        // 这几项是 Burp 定位一条记录的依据，缺任何一项导入回去都会指错目标。
        assertTrue(encoded.contains("<items exportTime="))
        assertTrue(encoded.contains("<item>"))
        assertTrue(encoded.contains("<host>api.example.com</host>"))
        assertTrue(encoded.contains("<port>8443</port>"))
        assertTrue(encoded.contains("<protocol>https</protocol>"))
        assertTrue(encoded.contains("<status>200</status>"))
        assertTrue(encoded.contains("""<request base64="true">"""))
        assertTrue(encoded.contains("""<response base64="true">"""))
    }

    @Test
    fun `an encoded document does not claim to be a specific burp build`() {
        val encoded = BurpItemsXmlCodec.encode(items = listOf(ITEM), exportedAt = EXPORTED_AT)

        // 这里不是 Burp，编一个版本号会让人以为文件出自某个具体版本，排查时是假线索。
        assertTrue(!encoded.contains("burpVersion"))
    }

    @Test
    fun `a round trip keeps every field and both message bodies`() {
        val decoded =
            BurpItemsXmlCodec.decode(
                BurpItemsXmlCodec.encode(listOf(ITEM), EXPORTED_AT),
            )

        assertEquals(listOf(ITEM), decoded)
    }

    @Test
    fun `a round trip keeps carriage returns and non ascii bytes`() {
        val item =
            ITEM.copy(
                requestText = "POST /submit HTTP/1.1\r\nContent-Type: text/plain\r\n\r\n名字=小哪吒",
                responseText = "HTTP/1.1 204 No Content\r\n\r\n",
            )

        val decoded =
            BurpItemsXmlCodec.decode(
                BurpItemsXmlCodec.encode(listOf(item), EXPORTED_AT),
            )

        // 报文是按字节 base64 的，因此 CRLF 与非 ASCII 都必须原样回来；少一个 \r 请求就不再是同一个请求。
        assertEquals(item, decoded.single())
    }

    @Test
    fun `urls with xml metacharacters survive the round trip`() {
        val item = ITEM.copy(url = "https://api.example.com/search?q=a&b=<c>", path = "/search?q=a&b=<c>")

        val decoded =
            BurpItemsXmlCodec.decode(
                BurpItemsXmlCodec.encode(listOf(item), EXPORTED_AT),
            )

        assertEquals(item, decoded.single())
    }

    @Test
    fun `a document written in burp's own shape is read back`() {
        // 先算好再插值：把调用写在字符串模板里的话，格式化规则会去检查那串参数，读起来也更绕。
        val requestBase64 = base64("GET /api/user HTTP/1.1\r\nHost: api.example.com\r\n\r\n")
        val responseBase64 = base64("HTTP/1.1 200 OK\r\n\r\n{}")

        // 这份文档是手写的，不经过我们的编码器：验的是「读得懂别人写的」，而不是「读得懂自己写的」。
        val document =
            """
            <?xml version="1.0"?>
            <items burpVersion="2024.1" exportTime="Sun Sep 13 19:00:00 UTC 2026">
              <item>
                <time>Sun Sep 13 19:00:00 UTC 2026</time>
                <url><![CDATA[https://api.example.com/api/user]]></url>
                <host ip="203.0.113.10">api.example.com</host>
                <port>443</port>
                <protocol>https</protocol>
                <method><![CDATA[GET]]></method>
                <path><![CDATA[/api/user]]></path>
                <extension>null</extension>
                <request base64="true"><![CDATA[$requestBase64]]></request>
                <status>200</status>
                <responselength>1024</responselength>
                <mimetype>JSON</mimetype>
                <response base64="true"><![CDATA[$responseBase64]]></response>
                <comment><![CDATA[]]></comment>
              </item>
            </items>
            """.trimIndent()

        val decoded = BurpItemsXmlCodec.decode(document)

        assertEquals(
            BurpItem(
                url = "https://api.example.com/api/user",
                host = "api.example.com",
                port = 443,
                protocol = "https",
                method = "GET",
                path = "/api/user",
                statusCode = 200,
                responseLength = 1024,
                mimeType = "JSON",
                requestText = "GET /api/user HTTP/1.1\r\nHost: api.example.com\r\n\r\n",
                responseText = "HTTP/1.1 200 OK\r\n\r\n{}",
            ),
            decoded.single(),
        )
    }

    @Test
    fun `an item without a request is skipped and the rest still decode`() {
        val secondRequestBase64 = base64("GET /2 HTTP/1.1\r\n\r\n")
        val document =
            """
            <items>
              <item><url><![CDATA[https://a.example.com/1]]></url><host>a.example.com</host><port>443</port><protocol>https</protocol><method>GET</method><path>/1</path></item>
              <item><url><![CDATA[https://b.example.com/2]]></url><host>b.example.com</host><port>443</port><protocol>https</protocol><method>GET</method><path>/2</path><request base64="true"><![CDATA[$secondRequestBase64]]></request></item>
            </items>
            """.trimIndent()

        val decoded = BurpItemsXmlCodec.decode(document)

        // 没有请求的条目导进来也没有用，但不该因此丢掉整份文件。
        assertEquals(1, decoded.size)
        assertEquals("b.example.com", decoded.single().host)
    }

    @Test
    fun `a plain text payload is accepted as well as base64`() {
        val document =
            """
            <items>
              <item><host>a.example.com</host><port>80</port><protocol>http</protocol><method>GET</method><path>/1</path><request>GET /1 HTTP/1.1</request></item>
            </items>
            """.trimIndent()

        assertEquals("GET /1 HTTP/1.1", BurpItemsXmlCodec.decode(document).single().requestText)
    }

    @Test
    fun `text that is not xml is rejected instead of reading as an empty history`() {
        // 坏文件与空历史对用户是两件事，静默给空表会让人以为「打开成功了，只是里面没东西」。
        assertThrows(IllegalArgumentException::class.java) { BurpItemsXmlCodec.decode("this is not xml") }
    }

    @Test
    fun `a document whose root is not items is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { BurpItemsXmlCodec.decode("<history></history>") }
    }

    @Test
    fun `a document declaring a doctype is rejected before entities can be resolved`() {
        // 文件来自别的应用，内容不可信；不挡住 DTD 的话一个构造过的文件就能让解析器去读本机文件。
        val maliciousDocument =
            """
            <?xml version="1.0"?>
            <!DOCTYPE items [<!ENTITY secret SYSTEM "file:///etc/hosts">]>
            <items><item><request>&secret;</request></item></items>
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) { BurpItemsXmlCodec.decode(maliciousDocument) }
    }

    @Test
    fun `the file extension is derived from the path`() {
        val withExtension = BurpItemsXmlCodec.encode(listOf(ITEM.copy(path = "/assets/app.js")), EXPORTED_AT)
        val withoutExtension = BurpItemsXmlCodec.encode(listOf(ITEM.copy(path = "/api/user")), EXPORTED_AT)

        assertTrue(withExtension.contains("<extension>js</extension>"))
        assertTrue(withoutExtension.contains("<extension>null</extension>"))
    }

    private fun base64(text: String): String = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

    private companion object {
        val EXPORTED_AT: Instant = Instant.parse("2026-09-13T19:00:00Z")

        val ITEM =
            BurpItem(
                url = "https://api.example.com/api/user",
                host = "api.example.com",
                port = 8443,
                protocol = "https",
                method = "GET",
                path = "/api/user",
                statusCode = 200,
                responseLength = 1024,
                mimeType = "JSON",
                requestText = "GET /api/user HTTP/1.1\r\nHost: api.example.com\r\n\r\n",
                responseText = "HTTP/1.1 200 OK\r\n\r\n{}",
            )
    }
}
