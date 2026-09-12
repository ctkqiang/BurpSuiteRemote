package xin.ctkqiang.burpsuite.remote.mobileapp.domain.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeModeTest {
    @Test
    fun `every mode survives a write and read`() {
        for (themeMode in ThemeMode.entries) {
            assertEquals(themeMode, ThemeMode.fromStorageValue(themeMode.storageValue))
        }
    }

    @Test
    fun `an unknown value falls back to following the system`() {
        assertEquals(ThemeMode.Automatic, ThemeMode.fromStorageValue("midnight-neon"))
    }

    @Test
    fun `a missing value falls back to following the system`() {
        assertEquals(ThemeMode.Automatic, ThemeMode.fromStorageValue(null))
    }

    @Test
    fun `parsing is case insensitive`() {
        assertEquals(ThemeMode.Dark, ThemeMode.fromStorageValue("DARK"))
    }

    @Test
    fun `the stored values do not collide with each other`() {
        assertEquals(ThemeMode.entries.size, ThemeMode.storageValues.toSet().size)
        assertTrue(ThemeMode.storageValues.none { storageValue -> storageValue.isBlank() })
    }
}
