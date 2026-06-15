package com.example.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the Pomodoro countdown so it keeps running while the app
 * is backgrounded or the screen is off. UI reads state from the static [state] flow; commands
 * arrive as intent actions (from the Activity and from notification buttons). Phase durations
 * and cycle length come from [PomodoroSettings].
 */
class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "pomodoro_timer"
        const val NOTIF_ID = 1

        const val ACTION_START = "com.example.pomodoro.START"
        const val ACTION_PAUSE = "com.example.pomodoro.PAUSE"
        const val ACTION_RESET = "com.example.pomodoro.RESET"
        const val ACTION_SKIP = "com.example.pomodoro.SKIP"

        private val _state = MutableStateFlow(TimerUiState())
        val state: StateFlow<TimerUiState> = _state.asStateFlow()

        /**
         * Refresh from persistent storage: today's count always, and—when idle—re-apply the
         * configured duration for the current phase (so settings changes take effect, and a day
         * rollover updates the count). Safe to call from the UI.
         */
        fun syncFromStorage(context: Context) {
            val settings = PomodoroSettings(context)
            val today = PomodoroStats(context).getTodayCount()
            val s = _state.value
            _state.value = if (s.running) {
                s.copy(totalFocus = today, cycleLength = settings.focusBeforeLongBreak)
            } else {
                val dur = settings.durationMillis(s.phase)
                s.copy(
                    remainingMillis = dur,
                    totalMillis = dur,
                    totalFocus = today,
                    cycleLength = settings.focusBeforeLongBreak
                )
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null

    /** elapsedRealtime at which the current phase ends (only meaningful while running). */
    private var endAt: Long = 0L
    private var lastShownSecond: Long = -1L

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var stats: PomodoroStats
    private lateinit var settings: PomodoroSettings

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        stats = PomodoroStats(this)
        settings = PomodoroSettings(this)
        val s = _state.value
        if (!s.running) {
            val dur = settings.durationMillis(s.phase)
            _state.value = s.copy(
                remainingMillis = dur,
                totalMillis = dur,
                totalFocus = stats.getTodayCount(),
                cycleLength = settings.focusBeforeLongBreak
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESET -> resetTimer()
            ACTION_SKIP -> advanceToNext(alert = false, autoStart = _state.value.running)
        }
        return START_STICKY
    }

    private fun startTimer() {
        if (_state.value.running) return
        endAt = SystemClock.elapsedRealtime() + _state.value.remainingMillis
        _state.value = _state.value.copy(running = true)
        lastShownSecond = -1L
        startForeground(NOTIF_ID, buildNotification(_state.value))
        acquireWake()
        startTicking()
    }

    private fun pauseTimer() {
        if (!_state.value.running) return
        tickJob?.cancel()
        val remaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
        _state.value = _state.value.copy(running = false, remainingMillis = remaining)
        releaseWake()
        updateNotification()
    }

    private fun resetTimer() {
        tickJob?.cancel()
        endAt = 0L
        lastShownSecond = -1L
        releaseWake()
        val dur = settings.durationMillis(Phase.FOCUS)
        // Keep today's total; reset the current timer and the in-cycle counter.
        _state.value = TimerUiState(
            remainingMillis = dur,
            totalMillis = dur,
            totalFocus = _state.value.totalFocus,
            cycleLength = settings.focusBeforeLongBreak
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Move to the next phase (on natural completion or a skip). */
    private fun advanceToNext(alert: Boolean, autoStart: Boolean) {
        val current = _state.value
        if (alert) playAlert()

        val (next, completedFocus) = nextPhase(current)
        // Only a naturally completed focus (not a skip) is recorded.
        val total = if (alert && current.phase == Phase.FOCUS) stats.incrementToday()
        else current.totalFocus
        val duration = settings.durationMillis(next)
        val newState = current.copy(
            phase = next,
            remainingMillis = duration,
            totalMillis = duration,
            running = autoStart,
            completedFocus = completedFocus,
            totalFocus = total,
            cycleLength = settings.focusBeforeLongBreak
        )
        _state.value = newState
        lastShownSecond = -1L

        if (autoStart) {
            endAt = SystemClock.elapsedRealtime() + duration
            startForeground(NOTIF_ID, buildNotification(newState))
            acquireWake()
            if (tickJob?.isActive != true) startTicking()
        } else {
            tickJob?.cancel()
            releaseWake()
            updateNotification()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val remaining = endAt - SystemClock.elapsedRealtime()
                if (remaining <= 0) {
                    advanceToNext(alert = true, autoStart = true)
                    continue
                }
                _state.value = _state.value.copy(remainingMillis = remaining)
                val second = remaining / 1000
                if (second != lastShownSecond) {
                    lastShownSecond = second
                    updateNotification()
                }
                delay(200)
            }
        }
    }

    private fun playAlert() {
        vibrate()
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri)?.play()
        } catch (_: Exception) {
            // A missing or silent default ringtone must not crash the timer.
        }
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 400, 200, 400)
        val effect = VibrationEffect.createWaveform(pattern, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(effect)
        }
    }

    private fun acquireWake() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val lock = wakeLock ?: pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pomodoro:timer")
            .also { wakeLock = it }
        if (!lock.isHeld) lock.acquire(60 * 60 * 1000L) // 1h safety cap
    }

    private fun releaseWake() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW // we play our own end-of-phase sound
        ).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(s: TimerUiState): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val toggleAction = if (s.running) ACTION_PAUSE else ACTION_START
        val toggleLabel = getString(if (s.running) R.string.action_pause else R.string.action_start)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("${getString(s.phase.labelRes)} · ${formatTime(s.remainingMillis)}")
            .setContentText(getString(R.string.notif_cycle, s.completedFocus, s.cycleLength))
            .setContentIntent(openIntent)
            .setOngoing(s.running)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, toggleLabel, servicePending(toggleAction, 1))
            .addAction(0, getString(R.string.action_skip), servicePending(ACTION_SKIP, 2))
            .addAction(0, getString(R.string.action_reset), servicePending(ACTION_RESET, 3))
            .build()
    }

    private fun servicePending(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, TimerService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(_state.value))
    }

    override fun onDestroy() {
        tickJob?.cancel()
        releaseWake()
        scope.cancel()
        super.onDestroy()
    }
}
