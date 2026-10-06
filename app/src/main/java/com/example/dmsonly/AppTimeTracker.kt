package com.example.dmsonly

import android.content.SharedPreferences
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppTimeSnapshot(
    val todaySeconds: Long,
    val yesterdaySeconds: Long,
    val last7DaysSeconds: Long
)

class AppTimeTracker(
    private val preferences: SharedPreferences,
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() },
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val PREFERENCES = "app_time_preferences"
        private const val DAY_PREFIX = "day_"
        private const val DATE_PATTERN = "yyyy-MM-dd"
    }

    private var startedAtElapsed: Long? = null
    private var startedDay: String? = null

    @Synchronized
    fun start() {
        if (startedAtElapsed != null) return
        startedAtElapsed = elapsedRealtime()
        startedDay = dayKey(nowMillis())
    }

    @Synchronized
    fun stop() {
        val started = startedAtElapsed ?: return
        val day = startedDay ?: dayKey(nowMillis())
        val duration = ((elapsedRealtime() - started).coerceAtLeast(0L)) / 1000L
        addSeconds(day, duration)
        startedAtElapsed = null
        startedDay = null
    }

    @Synchronized
    fun snapshot(): AppTimeSnapshot {
        persistCurrentSegment()
        val today = dayKey(nowMillis())
        val yesterday = dayKey(offsetDay(nowMillis(), -1))
        var sevenDays = 0L
        for (offset in 0 downTo -6) {
            sevenDays += preferences.getLong(DAY_PREFIX + dayKey(offsetDay(nowMillis(), offset)), 0L)
        }
        return AppTimeSnapshot(
            todaySeconds = preferences.getLong(DAY_PREFIX + today, 0L),
            yesterdaySeconds = preferences.getLong(DAY_PREFIX + yesterday, 0L),
            last7DaysSeconds = sevenDays
        )
    }

    @Synchronized
    fun reset() {
        preferences.edit().clear().apply()
        startedAtElapsed = null
        startedDay = null
    }

    private fun persistCurrentSegment() {
        val started = startedAtElapsed ?: return
        val day = startedDay ?: return
        val duration = ((elapsedRealtime() - started).coerceAtLeast(0L)) / 1000L
        if (duration > 0) {
            addSeconds(day, duration)
            startedAtElapsed = elapsedRealtime()
            startedDay = dayKey(nowMillis())
        }
    }

    private fun addSeconds(day: String, seconds: Long) {
        if (seconds <= 0) return
        val key = DAY_PREFIX + day
        preferences.edit().putLong(key, preferences.getLong(key, 0L) + seconds).apply()
    }

    private fun dayKey(millis: Long): String =
        SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date(millis))

    private fun offsetDay(millis: Long, offset: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return calendar.timeInMillis
    }
}

fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "%dh %02dm".format(Locale.US, hours, minutes)
        minutes > 0 -> "%dm %02ds".format(Locale.US, minutes, seconds)
        else -> "%ds".format(Locale.US, seconds)
    }
}
