package com.example.dmsonly.ui.theme

enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun resolveDarkTheme(systemDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStoredValue(value: String?): AppearanceMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
