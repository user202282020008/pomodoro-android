package com.example.pomodoro

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/** Applies the saved day/night theme as early as possible. */
class PomodoroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(nightModeFor(PomodoroSettings(this).themeMode))
    }

    companion object {
        fun nightModeFor(themeMode: Int): Int = when (themeMode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
