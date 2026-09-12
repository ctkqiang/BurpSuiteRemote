package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 主题里被点名要求的几条视觉事实。
 *
 * 这些断言替掉了「装到机器上凭肉眼确认」：底色到底是纯白还是带灰的「白」、深色是不是纯黑，
 * 只差几个十六进制位，截图里看不出来，但读数一眼就能分。
 */
class BurpRemoteThemeTest {
    @Test
    fun `the light background is pure white`() {
        assertEquals(Color(0xFFFFFFFF), BurpRemoteLightColourScheme.background)
    }

    @Test
    fun `the dark background is a lifted near black`() {
        val background = BurpRemoteDarkColourScheme.background
        // 纯黑会让相邻层级差不可见，也会让 OLED 上的滚动拖影更明显，因此这里必须不是纯黑。
        assertNotEquals(Color(0xFF000000), background)
        assertTrue(brightestChannelOf(background) <= NEAR_BLACK_CHANNEL_LIMIT)
        assertTrue(brightestChannelOf(background) > PURE_BLACK_HEADROOM)
    }

    @Test
    fun `the dark background is neutral`() {
        // 中性色：三通道彼此接近；偏蓝的「黑」在深色主题里会显得脏。
        assertTrue(channelSpreadOf(BurpRemoteDarkColourScheme.background) <= NEUTRAL_CHANNEL_TOLERANCE)
    }

    @Test
    fun `both schemes take the accent from the single brand token`() {
        assertEquals(BurpRemoteColour, BurpRemoteLightColourScheme.accent)
        assertEquals(BurpRemoteColour, BurpRemoteDarkColourScheme.accent)
    }

    @Test
    fun `every colour slot is assigned in both schemes`() {
        for (scheme in listOf(BurpRemoteLightColourScheme, BurpRemoteDarkColourScheme)) {
            val slots =
                listOf(
                    scheme.background,
                    scheme.surface,
                    scheme.surfaceElevated,
                    scheme.outline,
                    scheme.contentPrimary,
                    scheme.contentSecondary,
                    scheme.accent,
                    scheme.onAccent,
                    scheme.success,
                    scheme.warning,
                    scheme.danger,
                    scheme.information,
                )
            // 漏配的槽位会取到 Color.Unspecified，它画出来是透明；这里把「漏配」变成一条断言。
            assertTrue(slots.none { colour -> colour == Color.Unspecified })
            assertTrue(slots.all { colour -> colour.alpha == OPAQUE_ALPHA })
        }
    }

    @Test
    fun `semantic colours stay readable against the background they sit on`() {
        for (scheme in listOf(BurpRemoteLightColourScheme, BurpRemoteDarkColourScheme)) {
            val semantics = listOf(scheme.success, scheme.warning, scheme.danger, scheme.information)
            assertTrue(semantics.none { colour -> colour == scheme.background })
            assertTrue(semantics.none { colour -> colour == scheme.surface })
        }
    }

    @Test
    fun `the two backgrounds sit on opposite sides of the brightness threshold`() {
        // 这条把「配色」和系统栏图标的明暗绑在一起：底色亮就该配深色图标，反之亦然。
        assertTrue(brightestChannelOf(BurpRemoteLightColourScheme.background) > BACKGROUND_LIGHT_LIMIT)
        assertTrue(brightestChannelOf(BurpRemoteDarkColourScheme.background) <= BACKGROUND_LIGHT_LIMIT)
    }
}

private fun brightestChannelOf(colour: Color): Float = maxOf(colour.red, colour.green, colour.blue)

private fun channelSpreadOf(colour: Color): Float =
    brightestChannelOf(colour) - minOf(colour.red, colour.green, colour.blue)

private const val OPAQUE_ALPHA = 1f
private const val NEAR_BLACK_CHANNEL_LIMIT = 0.1f
private const val PURE_BLACK_HEADROOM = 0.02f
private const val NEUTRAL_CHANNEL_TOLERANCE = 0.03f
private const val BACKGROUND_LIGHT_LIMIT = 0.5f
