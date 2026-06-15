package com.example.pomodoro

import android.content.Context

/** User-configurable durations, theme and white-noise options, persisted in SharedPreferences. */
class PomodoroSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pomodoro_settings", Context.MODE_PRIVATE)

    val focusMinutes: Int get() = prefs.getInt(KEY_FOCUS, 25)
    val shortBreakMinutes: Int get() = prefs.getInt(KEY_SHORT, 5)
    val longBreakMinutes: Int get() = prefs.getInt(KEY_LONG, 15)
    val focusBeforeLongBreak: Int get() = prefs.getInt(KEY_CYCLE, 4)

    /** 0 = follow system, 1 = light, 2 = dark. */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME, 0)
        set(value) { prefs.edit().putInt(KEY_THEME, value).apply() }

    var whiteNoiseEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOISE_ON, false)
        set(value) { prefs.edit().putBoolean(KEY_NOISE_ON, value).apply() }

    /** 0 = white noise, 1 = brown noise. */
    var noiseType: Int
        get() = prefs.getInt(KEY_NOISE_TYPE, 0)
        set(value) { prefs.edit().putInt(KEY_NOISE_TYPE, value).apply() }

    fun durationMillis(phase: Phase): Long = when (phase) {
        Phase.FOCUS -> focusMinutes
        Phase.SHORT_BREAK -> shortBreakMinutes
        Phase.LONG_BREAK -> longBreakMinutes
    } * 60_000L

    fun saveDurations(focus: Int, short: Int, long: Int, cycle: Int) {
        prefs.edit()
            .putInt(KEY_FOCUS, focus.coerceIn(1, 180))
            .putInt(KEY_SHORT, short.coerceIn(1, 60))
            .putInt(KEY_LONG, long.coerceIn(1, 60))
            .putInt(KEY_CYCLE, cycle.coerceIn(1, 12))
            .apply()
    }

    companion object {
        private const val KEY_FOCUS = "focus_min"
        private const val KEY_SHORT = "short_min"
        private const val KEY_LONG = "long_min"
        private const val KEY_CYCLE = "cycle_len"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_NOISE_ON = "noise_on"
        private const val KEY_NOISE_TYPE = "noise_type"
    }
}
