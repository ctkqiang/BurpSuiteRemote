package xin.ctkqiang.burpsuite.remote.mobileapp.ui.theme

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 系统栏图标明暗映射的断言。
 *
 * 映射错了的表现是「状态栏的字和背景一个颜色」，只有真机上才看得见，因此这里把它钉死在纯函数上。
 */
class SystemBarIconAppearanceTest {
    @Test
    fun `the light theme asks for dark system bar icons`() {
        val appearance = SystemBarIconAppearance.of(isDarkTheme = false)

        assertTrue(appearance.isLightStatusBar)
        assertTrue(appearance.isLightNavigationBar)
    }

    @Test
    fun `the dark theme asks for light system bar icons`() {
        val appearance = SystemBarIconAppearance.of(isDarkTheme = true)

        assertFalse(appearance.isLightStatusBar)
        assertFalse(appearance.isLightNavigationBar)
    }

    @Test
    fun `both system bars always follow the same direction`() {
        for (isDarkTheme in listOf(true, false)) {
            val appearance = SystemBarIconAppearance.of(isDarkTheme)

            assertEquals(appearance.isLightStatusBar, appearance.isLightNavigationBar)
        }
    }

    @Test
    fun `the mapping is the inverse of the theme darkness`() {
        // 明暗是反的：深色主题配浅色图标。写成正向会在浅色主题下把状态栏的字抹掉。
        for (isDarkTheme in listOf(true, false)) {
            assertEquals(!isDarkTheme, SystemBarIconAppearance.of(isDarkTheme).isLightStatusBar)
        }
    }
}
