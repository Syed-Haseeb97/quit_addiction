package com.example.dmsonly

import java.time.LocalTime
import org.junit.Assert.assertFalse
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
        assertTrue(settings.isQuietHoursActive(LocalTime.of(23, 30)))
        assertTrue(settings.isQuietHoursActive(LocalTime.of(2, 0)))
        assertFalse(settings.isQuietHoursActive(LocalTime.of(12, 0)))
    }

    @Test
    fun daytimeQuietHoursWorks() {
        val settings = WellbeingSettings(
            quietHoursEnabled = true,
            quietHoursStart = "09:00",
            quietHoursEnd = "17:00"
        )
        assertTrue(settings.isQuietHoursActive(LocalTime.of(12, 0)))
        assertFalse(settings.isQuietHoursActive(LocalTime.of(18, 0)))
    }

    @Test
    fun invalidTimesDoNotAccidentallyBlock() {
        val settings = WellbeingSettings(
            quietHoursEnabled = true,
            quietHoursStart = "not-a-time",
            quietHoursEnd = "07:00"
        )
        assertFalse(settings.isQuietHoursActive(LocalTime.NOON))
    }
}
