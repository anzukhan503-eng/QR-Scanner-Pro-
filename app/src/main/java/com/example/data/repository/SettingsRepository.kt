package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qr_scanner_settings", Context.MODE_PRIVATE)

    private val _notificationsEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _dailyTipsEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_DAILY_TIPS_ENABLED, true))
    val dailyTipsEnabled: StateFlow<Boolean> = _dailyTipsEnabled.asStateFlow()

    private val _darkModeTheme =
        MutableStateFlow(prefs.getString(KEY_DARK_MODE, "SYSTEM") ?: "SYSTEM")
    val darkModeTheme: StateFlow<String> = _darkModeTheme.asStateFlow()

    private val _vibrateOnScan =
        MutableStateFlow(prefs.getBoolean(KEY_VIBRATE_ON_SCAN, true))
    val vibrateOnScan: StateFlow<Boolean> = _vibrateOnScan.asStateFlow()

    private val _beepOnScan =
        MutableStateFlow(prefs.getBoolean(KEY_BEEP_ON_SCAN, true))
    val beepOnScan: StateFlow<Boolean> = _beepOnScan.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setDailyTipsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_TIPS_ENABLED, enabled).apply()
        _dailyTipsEnabled.value = enabled
    }

    fun setDarkModeTheme(theme: String) {
        prefs.edit().putString(KEY_DARK_MODE, theme).apply()
        _darkModeTheme.value = theme
    }

    fun setVibrateOnScan(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATE_ON_SCAN, enabled).apply()
        _vibrateOnScan.value = enabled
    }

    fun setBeepOnScan(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BEEP_ON_SCAN, enabled).apply()
        _beepOnScan.value = enabled
    }

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DAILY_TIPS_ENABLED = "daily_tips_enabled"
        private const val KEY_DARK_MODE = "dark_mode_theme"
        private const val KEY_VIBRATE_ON_SCAN = "vibrate_on_scan"
        private const val KEY_BEEP_ON_SCAN = "beep_on_scan"
    }
}
