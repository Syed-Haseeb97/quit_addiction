package com.example.dmsonly

import org.junit.Assert.assertEquals
import org.junit.Test

class AppTimeTrackerTest {

    @Test
    fun formatsSecondsAndMinutes() {
        assertEquals("0s", formatDuration(0))
        assertEquals("7s", formatDuration(7))
        assertEquals("12m 05s", formatDuration(725))
    }

    @Test
    fun formatsHoursWithoutShowingTotalSeconds() {
        assertEquals("1h 01m", formatDuration(3665))
        assertEquals("24h 00m", formatDuration(86400))
    }
}
