package com.example.dmsonly

import android.content.Context
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class WellbeingSettings(
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "23:00",
    val quietHoursEnd: String = "07:00",
    val dopamineFreeUi: Boolean = false,
    val ghostMode: Boolean = false,
) {
    fun isQuietHoursActive(now: LocalTime = LocalTime.now()): Boolean {
        if (!quietHoursEnabled) return false
        val start = parseTime(quietHoursStart) ?: return false
        val end = parseTime(quietHoursEnd) ?: return false
        if (start == end) return true
        return if (start < end) now >= start && now < end else now >= start || now < end
    }

    companion object {
        private const val PREFS = "wellbeing_preferences"
        private const val QUIET_ENABLED = "quiet_hours_enabled"
        private const val QUIET_START = "quiet_hours_start"
        private const val QUIET_END = "quiet_hours_end"
        private const val DOPAMINE_FREE = "dopamine_free_ui"
        private const val GHOST_MODE = "ghost_mode"

        private val formatter = DateTimeFormatter.ofPattern("HH:mm")

        fun load(context: Context): WellbeingSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return WellbeingSettings(
                quietHoursEnabled = p.getBoolean(QUIET_ENABLED, false),
                quietHoursStart = p.getString(QUIET_START, "23:00") ?: "23:00",
                quietHoursEnd = p.getString(QUIET_END, "07:00") ?: "07:00",
                dopamineFreeUi = p.getBoolean(DOPAMINE_FREE, false),
                ghostMode = p.getBoolean(GHOST_MODE, false),
            )
        }

        fun save(context: Context, settings: WellbeingSettings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(QUIET_ENABLED, settings.quietHoursEnabled)
                .putString(QUIET_START, settings.quietHoursStart)
                .putString(QUIET_END, settings.quietHoursEnd)
                .putBoolean(DOPAMINE_FREE, settings.dopamineFreeUi)
                .putBoolean(GHOST_MODE, settings.ghostMode)
                .apply()
        }

        fun parseTime(value: String): LocalTime? = try {
            LocalTime.parse(value, formatter)
        } catch (_: DateTimeParseException) {
            null
        }

        fun normalizeTime(value: String): String? =
            parseTime(value.trim())?.format(formatter)
    }
}
