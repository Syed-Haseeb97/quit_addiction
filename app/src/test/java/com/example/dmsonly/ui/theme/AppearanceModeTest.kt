package com.example.dmsonly.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceModeTest {

    @Test
    fun systemModeFollowsSystemAppearance() {
        assertTrue(AppearanceMode.SYSTEM.resolveDarkTheme(systemDarkTheme = true))
        assertFalse(AppearanceMode.SYSTEM.resolveDarkTheme(systemDarkTheme = false))
    }

    @Test
    fun lightAndDarkModesOverrideSystemAppearance() {
        assertFalse(AppearanceMode.LIGHT.resolveDarkTheme(systemDarkTheme = true))
        assertTrue(AppearanceMode.DARK.resolveDarkTheme(systemDarkTheme = false))
    }

    @Test
    fun preferenceValueRoundTripsAndInvalidValueDefaultsToSystem() {
        AppearanceMode.entries.forEach { mode ->
            assertEquals(mode, AppearanceMode.fromStoredValue(mode.name))
        }
        assertEquals(AppearanceMode.SYSTEM, AppearanceMode.fromStoredValue(null))
        assertEquals(AppearanceMode.SYSTEM, AppearanceMode.fromStoredValue("invalid"))
    }
}
