package com.example.pomodoro

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.pomodoro.databinding.ActivitySettingsBinding

/** Customise durations, day/night theme and white-noise type. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val settings = PomodoroSettings(this)
        binding.etFocus.setText(settings.focusMinutes.toString())
        binding.etShort.setText(settings.shortBreakMinutes.toString())
        binding.etLong.setText(settings.longBreakMinutes.toString())
        binding.etCycle.setText(settings.focusBeforeLongBreak.toString())

        binding.themeGroup.check(
            when (settings.themeMode) {
                1 -> R.id.themeLight
                2 -> R.id.themeDark
                else -> R.id.themeSystem
            }
        )
        binding.noiseGroup.check(
            if (settings.noiseType == 1) R.id.noiseBrown else R.id.noiseWhite
        )

        binding.btnSave.setOnClickListener {
            settings.saveDurations(
                binding.etFocus.text.toString().toIntOrNull() ?: 25,
                binding.etShort.text.toString().toIntOrNull() ?: 5,
                binding.etLong.text.toString().toIntOrNull() ?: 15,
                binding.etCycle.text.toString().toIntOrNull() ?: 4
            )
            val theme = when (binding.themeGroup.checkedButtonId) {
                R.id.themeLight -> 1
                R.id.themeDark -> 2
                else -> 0
            }
            settings.themeMode = theme
            settings.noiseType = if (binding.noiseGroup.checkedButtonId == R.id.noiseBrown) 1 else 0

            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            AppCompatDelegate.setDefaultNightMode(PomodoroApp.nightModeFor(theme))
            finish()
        }
    }
}
