package com.example.pomodoro

/** The three phases of the Pomodoro cycle. Durations come from [PomodoroSettings]. */
enum class Phase(val labelRes: Int) {
    FOCUS(R.string.phase_focus),
    SHORT_BREAK(R.string.phase_short_break),
    LONG_BREAK(R.string.phase_long_break)
}

/** Immutable snapshot of the timer, observed by the UI and the notification. */
data class TimerUiState(
    val phase: Phase = Phase.FOCUS,
    val remainingMillis: Long = 25 * 60 * 1000L,
    val totalMillis: Long = 25 * 60 * 1000L,
    val running: Boolean = false,
    /** Focus sessions completed in the current cycle (0..cycleLength). */
    val completedFocus: Int = 0,
    /** Focus sessions completed today (persisted). */
    val totalFocus: Int = 0,
    /** Number of focus sessions before a long break (from settings). */
    val cycleLength: Int = 4
)

/** Format milliseconds as MM:SS, rounding up so a fresh 25:00 shows 25:00 (not 24:59). */
fun formatTime(millis: Long): String {
    val totalSeconds = ((millis + 999) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/**
 * Compute the next phase after [current] completes (or is skipped),
 * paired with the new in-cycle focus count.
 */
fun nextPhase(current: TimerUiState): Pair<Phase, Int> = when (current.phase) {
    Phase.FOCUS -> {
        val done = current.completedFocus + 1
        if (done >= current.cycleLength) Phase.LONG_BREAK to done
        else Phase.SHORT_BREAK to done
    }
    Phase.SHORT_BREAK -> Phase.FOCUS to current.completedFocus
    Phase.LONG_BREAK -> Phase.FOCUS to 0 // long break finishes a cycle
}
