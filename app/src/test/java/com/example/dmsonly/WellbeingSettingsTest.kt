package com.example.dmsonly

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WellbeingSettingsTest {
    @Test
    fun overnightQuietHoursSpansMidnight() {
        val settings = WellbeingSettings(
            quietHoursEnabled = true,
            quietHoursStart = "23:00",
            quietHoursEnd = "07:00"
        )
        assertTrue(settings.isQuietHoursActive("23:30"))
        assertTrue(settings.isQuietHoursActive("02:00"))
        assertFalse(settings.isQuietHoursActive("12:00"))
    }

    @Test
    fun daytimeQuietHoursWorks() {
        val settings = WellbeingSettings(
            quietHoursEnabled = true,
            quietHoursStart = "09:00",
            quietHoursEnd = "17:00"
        )
        assertTrue(settings.isQuietHoursActive("12:00"))
        assertFalse(settings.isQuietHoursActive("18:00"))
    }

    @Test
    fun scheduleUsesInclusiveStartAndExclusiveEnd() {
        val settings = WellbeingSettings(
            quietHoursEnabled = true,
            quietHoursStart = "09:00",
            quietHoursEnd = "17:00"
        )

        assertTrue(settings.isQuietHoursActive("09:00"))
        assertFalse(settings.isQuietHoursActive("17:00"))
    }

    @Test
    fun invalidTimesDoNotAccidentallyBlock() {
        val settings = WellbeingSettings(
            quietHoursEnabled = true,
            quietHoursStart = "not-a-time",
            quietHoursEnd = "07:00"
        )
        assertFalse(settings.isQuietHoursActive("12:00"))
    }

    @Test
    fun invalidClockTimesAreRejectedAndEqualBoundsMeanAllDay() {
        assertNull(WellbeingSettings.parseTime("24:00"))
        assertNull(WellbeingSettings.parseTime("12:60"))
        assertEquals("09:05", WellbeingSettings.normalizeTime("09:05"))
        assertNull(WellbeingSettings.normalizeTime("9:05"))
        assertTrue(
            WellbeingSettings(
                quietHoursEnabled = true,
                quietHoursStart = "08:30",
                quietHoursEnd = "08:30"
            ).isQuietHoursActive("23:59")
        )
    }
}
