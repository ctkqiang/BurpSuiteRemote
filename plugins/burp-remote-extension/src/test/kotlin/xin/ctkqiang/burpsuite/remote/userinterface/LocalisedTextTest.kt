/**
 * 本地化测试：三份资源束的键集合、回落规则、UTF-8 解码与位置参数替换。
 * 语言全部显式传入；ResourceBundle 会把 JVM 默认语言环境插进候选链，坑就在这。
 */

package xin.ctkqiang.burpsuite.remote.userinterface

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.ResourceBundle

class LocalisedTextTest {
    @Test
    fun `every supported locale declares exactly the keys of the default bundle`() {
        val defaultKeys = bundleFor(Locale.ENGLISH).keySet()

        for (supportedLocale in supportedLocales) {
            assertEquals(defaultKeys, bundleFor(supportedLocale).keySet(), "语言环境：$supportedLocale")
        }
    }

    @Test
    fun `an unsupported locale falls back to the default bundle`() {
        val unsupportedLocaleText = LocalisedText.forLocale(Locale.FRENCH)

        assertEquals("Pairing code", unsupportedLocaleText.text(TextKey.PAIRING_CODE_CAPTION))
    }

    @Test
    fun `the resolution ignores the ambient default locale`() {
        val ambientLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE)

            assertEquals("Pairing code", LocalisedText.forLocale(Locale.ENGLISH).text(TextKey.PAIRING_CODE_CAPTION))
            assertEquals("配对码", LocalisedText.forLocale(Locale.SIMPLIFIED_CHINESE).text(TextKey.PAIRING_CODE_CAPTION))
        } finally {
            Locale.setDefault(ambientLocale)
        }
    }

    @Test
    fun `chinese text is decoded as utf 8 rather than latin 1`() {
        val chineseText = LocalisedText.forLocale(Locale.SIMPLIFIED_CHINESE)

        assertEquals("配对码", chineseText.text(TextKey.PAIRING_CODE_CAPTION))
        assertEquals("预留监听端口", chineseText.text(TextKey.REMOTE_PORT_LABEL))
    }

    @Test
    fun `german text keeps its umlauts`() {
        val germanText = LocalisedText.forLocale(Locale.GERMAN)

        assertEquals("Protokollversion", germanText.text(TextKey.PROTOCOL_VERSION_LABEL))
        assertTrue(germanText.text(TextKey.PAIRING_VALIDITY_REMAINING).startsWith("Gültig bis {0}"))
    }

    @Test
    fun `positional arguments are substituted in the active locale`() {
        val remainingDurationArguments = arrayOf(PAIRING_MINUTES, PAIRING_SECONDS)

        assertEquals(
            "4 min 12 sec",
            LocalisedText.forLocale(Locale.ENGLISH).format(TextKey.REMAINING_DURATION, *remainingDurationArguments),
        )
        assertEquals(
            "4 分 12 秒",
            LocalisedText.forLocale(Locale.SIMPLIFIED_CHINESE).format(
                TextKey.REMAINING_DURATION,
                *remainingDurationArguments,
            ),
        )
    }

    private fun bundleFor(locale: Locale): ResourceBundle =
        ResourceBundle.getBundle(
            LocalisedText.MESSAGES_BUNDLE_BASE_NAME,
            locale,
            LocalisedText::class.java.classLoader,
        )

    private companion object {
        private val supportedLocales = listOf(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE, Locale.GERMAN)

        private const val PAIRING_MINUTES = 4

        private const val PAIRING_SECONDS = 12
    }
}
