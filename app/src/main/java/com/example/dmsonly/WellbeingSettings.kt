package com.example.dmsonly

import android.content.Context
import java.util.Calendar
import java.util.Locale

data class WellbeingSettings(
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "23:00",
    val quietHoursEnd: String = "07:00",
    val dopamineFreeUi: Boolean = false,
) {
    fun isQuietHoursActive(now: String = currentTime()): Boolean {
        if (!quietHoursEnabled) return false
        val start = parseTime(quietHoursStart) ?: return false
        val end = parseTime(quietHoursEnd) ?: return false
        if (start == end) return true
        val current = parseTime(now) ?: return false
        return if (start < end) current >= start && current < end else current >= start || current < end
    }

    companion object {
        private const val PREFS = "wellbeing_preferences"
        private const val QUIET_ENABLED = "quiet_hours_enabled"
        private const val QUIET_START = "quiet_hours_start"
        private const val QUIET_END = "quiet_hours_end"
        private const val DOPAMINE_FREE = "dopamine_free_ui"

        fun load(context: Context): WellbeingSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return WellbeingSettings(
                quietHoursEnabled = p.getBoolean(QUIET_ENABLED, false),
                quietHoursStart = p.getString(QUIET_START, "23:00") ?: "23:00",
                quietHoursEnd = p.getString(QUIET_END, "07:00") ?: "07:00",
                dopamineFreeUi = p.getBoolean(DOPAMINE_FREE, false),
            )
        }

        fun save(context: Context, settings: WellbeingSettings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(QUIET_ENABLED, settings.quietHoursEnabled)
                .putString(QUIET_START, settings.quietHoursStart)
                .putString(QUIET_END, settings.quietHoursEnd)
                .putBoolean(DOPAMINE_FREE, settings.dopamineFreeUi)
                .apply()
        }

        fun currentTime(): String {
            val calendar = Calendar.getInstance()
            return String.format(
                Locale.ROOT,
                "%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
        }

        fun parseTime(value: String): Int? {
            val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(value.trim()) ?: return null
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour * 60 + minute
        }

        fun normalizeTime(value: String): String? =
            parseTime(value.trim())?.let { minutes ->
                String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)
            }
    }
}
