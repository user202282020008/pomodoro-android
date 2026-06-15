package com.example.pomodoro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.pomodoro.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermissionIfNeeded()

        binding.btnToggle.setOnClickListener {
            if (TimerService.state.value.running) {
                sendAction(TimerService.ACTION_PAUSE, foreground = false)
            } else {
                sendAction(TimerService.ACTION_START, foreground = true)
            }
        }
        binding.btnSkip.setOnClickListener {
            sendAction(TimerService.ACTION_SKIP, foreground = false)
        }
        binding.btnReset.setOnClickListener {
            sendAction(TimerService.ACTION_RESET, foreground = false)
        }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TimerService.state.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reflect today's count and any duration/cycle changes made on the settings screen.
        TimerService.syncFromStorage(this)
    }

    private fun render(s: TimerUiState) {
        binding.tvPhase.setText(s.phase.labelRes)
        binding.tvTime.text = formatTime(s.remainingMillis)
        binding.btnToggle.setText(if (s.running) R.string.action_pause else R.string.action_start)
        binding.tvCycle.text =
            getString(R.string.cycle_format, s.completedFocus, s.cycleLength, s.totalFocus)

        val progress = if (s.totalMillis > 0)
            (((s.totalMillis - s.remainingMillis) * 1000) / s.totalMillis).toInt() else 0
        binding.progress.progress = progress

        val bgRes = when (s.phase) {
            Phase.FOCUS -> R.drawable.bg_focus
            Phase.SHORT_BREAK -> R.drawable.bg_short_break
            Phase.LONG_BREAK -> R.drawable.bg_long_break
        }
        binding.root.setBackgroundResource(bgRes)

        val accentRes = when (s.phase) {
            Phase.FOCUS -> R.color.focus
            Phase.SHORT_BREAK -> R.color.short_break
            Phase.LONG_BREAK -> R.color.long_break
        }
        binding.btnToggle.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        binding.btnToggle.setTextColor(ContextCompat.getColor(this, accentRes))
    }

    private fun sendAction(action: String, foreground: Boolean) {
        val intent = Intent(this, TimerService::class.java).setAction(action)
        if (foreground) ContextCompat.startForegroundService(this, intent)
        else startService(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
