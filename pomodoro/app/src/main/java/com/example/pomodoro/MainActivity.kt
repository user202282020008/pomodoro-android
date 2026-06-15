package com.example.pomodoro

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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

    private var lastPhase: Phase? = null
    private var lastRunning: Boolean? = null
    private var breathing: Animator? = null

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermissionIfNeeded()

        binding.btnToggle.setOnClickListener {
            if (TimerService.state.value.running) sendAction(TimerService.ACTION_PAUSE, false)
            else sendAction(TimerService.ACTION_START, true)
        }
        binding.btnSkip.setOnClickListener { sendAction(TimerService.ACTION_SKIP, false) }
        binding.btnReset.setOnClickListener { sendAction(TimerService.ACTION_RESET, false) }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnNoise.setOnClickListener { toggleNoise() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TimerService.state.collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        TimerService.syncFromStorage(this)
        updateNoiseIcon(PomodoroSettings(this).whiteNoiseEnabled)
    }

    private fun render(s: TimerUiState) {
        val phaseChanged = lastPhase != null && lastPhase != s.phase

        binding.tvPhase.setText(s.phase.labelRes)
        binding.tvTime.text = formatTime(s.remainingMillis)
        binding.btnToggle.setText(if (s.running) R.string.action_pause else R.string.action_start)
        binding.tvCycle.text =
            getString(R.string.cycle_format, s.completedFocus, s.cycleLength, s.totalFocus)

        val progress = if (s.totalMillis > 0)
            (((s.totalMillis - s.remainingMillis) * 1000) / s.totalMillis).toInt() else 0
        binding.progress.setProgressCompat(progress, !phaseChanged && s.running)

        binding.root.setBackgroundResource(
            when (s.phase) {
                Phase.FOCUS -> R.drawable.bg_focus
                Phase.SHORT_BREAK -> R.drawable.bg_short_break
                Phase.LONG_BREAK -> R.drawable.bg_long_break
            }
        )
        val accentRes = when (s.phase) {
            Phase.FOCUS -> R.color.focus
            Phase.SHORT_BREAK -> R.color.short_break
            Phase.LONG_BREAK -> R.color.long_break
        }
        binding.btnToggle.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        binding.btnToggle.setTextColor(ContextCompat.getColor(this, accentRes))

        if (phaseChanged) animatePhaseChange()
        lastPhase = s.phase
        if (lastRunning != s.running) {
            setBreathing(s.running)
            lastRunning = s.running
        }
    }

    private fun animatePhaseChange() {
        listOf(binding.tvPhase, binding.tvTime).forEach { v ->
            v.alpha = 0.2f
            v.animate().alpha(1f).setDuration(420).start()
        }
        binding.timerFrame.apply {
            scaleX = 0.9f
            scaleY = 0.9f
            animate().scaleX(1f).scaleY(1f).setDuration(420)
                .setInterpolator(AccelerateDecelerateInterpolator()).start()
        }
    }

    /** Gentle "breathing" pulse on the timer panel while the countdown runs. */
    private fun setBreathing(on: Boolean) {
        breathing?.cancel()
        breathing = null
        binding.timerFrame.scaleX = 1f
        binding.timerFrame.scaleY = 1f
        if (!on) return
        val sx = ObjectAnimator.ofFloat(binding.timerFrame, View.SCALE_X, 1f, 1.03f)
        val sy = ObjectAnimator.ofFloat(binding.timerFrame, View.SCALE_Y, 1f, 1.03f)
        listOf(sx, sy).forEach {
            it.repeatCount = ValueAnimator.INFINITE
            it.repeatMode = ValueAnimator.REVERSE
            it.duration = 2400
        }
        breathing = AnimatorSet().apply {
            playTogether(sx, sy)
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun toggleNoise() {
        val settings = PomodoroSettings(this)
        val enabled = !settings.whiteNoiseEnabled
        settings.whiteNoiseEnabled = enabled
        updateNoiseIcon(enabled)
        if (TimerService.state.value.running) sendAction(TimerService.ACTION_SYNC, false)
    }

    private fun updateNoiseIcon(enabled: Boolean) {
        binding.btnNoise.setImageResource(
            if (enabled) R.drawable.ic_noise_on else R.drawable.ic_noise_off
        )
        binding.btnNoise.alpha = if (enabled) 1f else 0.55f
    }

    override fun onDestroy() {
        breathing?.cancel()
        super.onDestroy()
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
