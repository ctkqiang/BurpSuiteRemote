package xin.ctkqiang.burpsuite.remote.mobileapp.data.burpfile

import org.w3c.dom.Element
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Burp XML 条目的读写。
 *
 * 取的是 Burp「Export items / Import items」用的那个形状，而不是它私有的 `.burp` 项目文件：
 * 后者是闭源序列化格式，第三方既写不出来也读不了。XML 这一套是社区真正在交换的载体，
 * Burp 自己也能重新导入，因此只有它谈得上「往返」。
 *
 * 两处刻意的取舍：
 * - **不写 `burpVersion`**。那个属性该填 Burp 自己的版本号，而这里不是 Burp —— 编一个版本
 *   会让人以为文件出自某个具体版本的 Burp，排查问题时这是假线索。
 * - **解析时关掉 DTD 与外部实体**。这个文件来自别的应用，内容不可信；不关掉的话，
 *   一个精心构造的文件就能让解析器去读本机文件或发外部请求（XXE）。
 */
object BurpItemsXmlCodec {
    /** 把若干条目写成 Burp 能导入的 XML。 */
    fun encode(
        items: List<BurpItem>,
        exportedAt: Instant,
    ): String {
        val exportTime = EXPORT_TIME_FORMATTER.format(exportedAt)
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>""").append(LINE_BREAK)
            append("<items exportTime=\"")
                .append(escapeAttribute(exportTime))
                .append("\">")
                .append(LINE_BREAK)
            items.forEach { item -> appendItem(item = item, occurredAt = exportTime) }
            append("</items>").append(LINE_BREAK)
        }
    }

    /**
     * 把 Burp 导出的 XML 读成条目。
     *
     * 文档本身读不懂时抛 [IllegalArgumentException] 而不是返回空表：一个坏文件和一份空的历史
     * 对用户是两件事，静默返回空表会让人以为「打开成功了，只是里面没东西」。
     * 单条 item 缺报文时跳过它 —— 没有请求的条目导进来也没有用，但不必因此丢掉整份文件。
     */
    fun decode(text: String): List<BurpItem> {
        val document =
            try {
                newDocumentBuilderFactory()
                    .newDocumentBuilder()
                    .parse(text.byteInputStream(Charsets.UTF_8))
            } catch (malformedDocument: Exception) {
                throw IllegalArgumentException("不是可解析的 XML 文档", malformedDocument)
            }

        val root =
            document.documentElement
                ?: throw IllegalArgumentException("XML 文档没有根元素")
        if (root.tagName != ITEMS_ELEMENT) {
            throw IllegalArgumentException("根元素是 <${root.tagName}>，不是 <$ITEMS_ELEMENT>")
        }

        val itemElements = root.getElementsByTagName(ITEM_ELEMENT)
        return (0 until itemElements.length).mapNotNull { index ->
            decodeItem(element = itemElements.item(index) as? Element ?: return@mapNotNull null)
        }
    }

    private fun StringBuilder.appendItem(
        item: BurpItem,
        occurredAt: String,
    ) {
        append("  <item>").append(LINE_BREAK)
        appendElement(name = "time", text = occurredAt)
        appendCdataElement(name = "url", text = item.url)
        appendElement(name = "host", text = item.host)
        appendElement(name = "port", text = item.port.toString())
        appendElement(name = "protocol", text = item.protocol)
        appendCdataElement(name = "method", text = item.method)
        appendCdataElement(name = "path", text = item.path)
        appendElement(name = "extension", text = extensionOf(path = item.path))
        appendBase64Element(name = "request", text = item.requestText)
        appendElement(name = "status", text = item.statusCode.toString())
        appendElement(name = "responselength", text = item.responseLength.toString())
        appendElement(name = "mimetype", text = item.mimeType)
        appendBase64Element(name = "response", text = item.responseText)
        appendCdataElement(name = "comment", text = "")
        append("  </item>").append(LINE_BREAK)
    }

    private fun StringBuilder.appendElement(
        name: String,
        text: String,
    ) {
        append("    <").append(name).append('>')
            .append(escapeText(text))
            .append("</").append(name).append('>')
            .append(LINE_BREAK)
    }

    // 可能带任意字符的字段走 CDATA：URL 里的 & 与查询串里的尖括号因此不必逐个转义。
    private fun StringBuilder.appendCdataElement(
        name: String,
        text: String,
    ) {
        append("    <").append(name).append('>')
            .append(cdata(text))
            .append("</").append(name).append('>')
            .append(LINE_BREAK)
    }

    private fun StringBuilder.appendBase64Element(
        name: String,
        text: String,
    ) {
        val encoded = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        append("    <").append(name).append(" base64=\"true\">")
            .append(cdata(encoded))
            .append("</").append(name).append('>')
            .append(LINE_BREAK)
    }

    private fun decodeItem(element: Element): BurpItem? {
        val requestText = readPayload(element = element, name = "request") ?: return null
        val responseText = readPayload(element = element, name = "response").orEmpty()
        return BurpItem(
            url = readCdataOrText(element = element, name = "url").orEmpty(),
            host = readCdataOrText(element = element, name = "host").orEmpty(),
            port = readCdataOrText(element = element, name = "port")?.toIntOrNull() ?: 0,
            protocol = readCdataOrText(element = element, name = "protocol").orEmpty(),
            method = readCdataOrText(element = element, name = "method").orEmpty(),
            path = readCdataOrText(element = element, name = "path").orEmpty(),
            statusCode = readCdataOrText(element = element, name = "status")?.toIntOrNull() ?: 0,
            responseLength = readCdataOrText(element = element, name = "responselength")?.toIntOrNull() ?: 0,
            mimeType = readCdataOrText(element = element, name = "mimetype").orEmpty(),
            requestText = requestText,
            responseText = responseText,
        )
    }

    // 报文既可能是 base64（Burp 导出时的默认做法），也可能有人手工写成明文，两种都要认。
    private fun readPayload(
        element: Element,
        name: String,
    ): String? {
        val payloadElements = element.getElementsByTagName(name)
        val payloadElement = payloadElements.item(0) as? Element ?: return null
        val rawText = payloadElement.textContent.orEmpty()
        if (rawText.isEmpty()) return null
        val isBase64 = payloadElement.getAttribute(BASE64_ATTRIBUTE) == TRUE_ATTRIBUTE_VALUE
        if (!isBase64) return rawText
        return try {
            String(Base64.getDecoder().decode(rawText), Charsets.UTF_8)
        } catch (notBase64: IllegalArgumentException) {
            // 声明了 base64 但内容不是 base64：当作明文收下，比整条丢掉更接近用户的意图。
            null
        }
    }

    private fun readCdataOrText(
        element: Element,
        name: String,
    ): String? {
        val elements = element.getElementsByTagName(name)
        val child = elements.item(0) as? Element ?: return null
        return child.textContent.orEmpty()
    }

    // 关掉 DTD、外部实体与 XInclude：文件来自别的应用，内容是未经验证的。
    private fun newDocumentBuilderFactory(): DocumentBuilderFactory {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature(DISALLOW_DOCTYPE_DECLARATION_FEATURE, true)
        factory.setFeature(EXTERNAL_GENERAL_ENTITIES_FEATURE, false)
        factory.setFeature(EXTERNAL_PARAMETER_ENTITIES_FEATURE, false)
        factory.isXIncludeAware = false
        factory.isExpandEntityReferences = false
        return factory
    }

    // CDATA 里不能出现 ]]> 这三个字符，出现时拆成两段 CDATA 接起来。
    private fun cdata(text: String): String =
        "<![CDATA[" + text.replace(CDATA_TERMINATOR, CDATA_TERMINATOR_SPLIT) + "]]>"

    private fun escapeText(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

    private fun escapeAttribute(text: String): String = escapeText(text).replace("\"", "&quot;")

    // Burp 在没有扩展名时写的就是字面量 null，照抄它的约定。
    private fun extensionOf(path: String): String {
        val lastSegment = path.substringAfterLast('/')
        val extension = lastSegment.substringAfterLast('.', missingDelimiterValue = "")
        return if (extension.isEmpty() || extension == lastSegment) EXTENSION_ABSENT else extension
    }

    private const val ITEMS_ELEMENT = "items"
    private const val ITEM_ELEMENT = "item"
    private const val BASE64_ATTRIBUTE = "base64"
    private const val TRUE_ATTRIBUTE_VALUE = "true"
    private const val EXTENSION_ABSENT = "null"
    private const val LINE_BREAK = "\n"
    private const val CDATA_TERMINATOR = "]]>"
    private const val CDATA_TERMINATOR_SPLIT = "]]]]><![CDATA[>"
    private const val DISALLOW_DOCTYPE_DECLARATION_FEATURE =
        "http://apache.org/xml/features/disallow-doctype-decl"
    private const val EXTERNAL_GENERAL_ENTITIES_FEATURE =
        "http://xml.org/sax/features/external-general-entities"
    private const val EXTERNAL_PARAMETER_ENTITIES_FEATURE =
        "http://xml.org/sax/features/external-parameter-entities"

    // Burp 的 time 字段是 java.util.Date.toString() 的形状；不照这个形状写，对方读不出时间。
    private val EXPORT_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
            .withZone(ZoneId.systemDefault())
}
