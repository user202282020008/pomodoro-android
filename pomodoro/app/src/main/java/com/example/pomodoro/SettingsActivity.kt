package com.example.pomodoro

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pomodoro.databinding.ActivitySettingsBinding

/** Lets the user customise focus / break durations and how many focuses precede a long break. */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val settings = PomodoroSettings(this)
        binding.etFocus.setText(settings.focusMinutes.toString())
        binding.etShort.setText(settings.shortBreakMinutes.toString())
        binding.etLong.setText(settings.longBreakMinutes.toString())
        binding.etCycle.setText(settings.focusBeforeLongBreak.toString())

        binding.btnSave.setOnClickListener {
            settings.save(
                binding.etFocus.text.toString().toIntOrNull() ?: 25,
                binding.etShort.text.toString().toIntOrNull() ?: 5,
                binding.etLong.text.toString().toIntOrNull() ?: 15,
                binding.etCycle.text.toString().toIntOrNull() ?: 4
            )
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
