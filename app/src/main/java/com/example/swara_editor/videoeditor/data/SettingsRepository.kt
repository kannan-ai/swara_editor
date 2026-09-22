package com.example.swara_editor.videoeditor.data

import android.content.Context

enum class AppTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    OLED("OLED Black")
}

data class AppSettings(
    val theme: AppTheme = AppTheme.DARK,
    val isOledEnabled: Boolean = false
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("swara_settings", Context.MODE_PRIVATE)

    fun getSettings(): AppSettings {
        val themeName = prefs.getString("theme_mode", AppTheme.DARK.name) ?: AppTheme.DARK.name
        val theme = runCatching { AppTheme.valueOf(themeName) }.getOrDefault(AppTheme.DARK)
        val isOled = prefs.getBoolean("is_oled_enabled", false)
        return AppSettings(theme = theme, isOledEnabled = isOled)
    }

    fun updateTheme(theme: AppTheme) {
        prefs.edit().putString("theme_mode", theme.name).apply()
    }

    fun updateOledEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_oled_enabled", enabled).apply()
    }
}
