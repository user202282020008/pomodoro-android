package com.example.pomodoro

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persists the number of completed focus sessions per day in SharedPreferences,
 * so today's count and the history survive the app being closed or killed.
 */
class PomodoroStats(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pomodoro_stats", Context.MODE_PRIVATE)

    fun today(): String = DATE_FORMAT.format(Date())

    fun getCount(date: String): Int = prefs.getInt(keyFor(date), 0)

    fun getTodayCount(): Int = getCount(today())

    /** Increment today's count, remember the date, and return the new value. */
    fun incrementToday(): Int {
        val date = today()
        val next = getCount(date) + 1
        val dates = LinkedHashSet(loadDates()).apply { add(date) }
        prefs.edit()
            .putInt(keyFor(date), next)
            .putString(KEY_DATES, dates.joinToString(","))
            .apply()
        return next
    }

    /** Recorded days, most recent first, paired with their counts. */
    fun history(): List<Pair<String, Int>> =
        loadDates().sortedDescending().map { it to getCount(it) }

    fun totalAllTime(): Int = loadDates().sumOf { getCount(it) }

    private fun loadDates(): List<String> =
        prefs.getString(KEY_DATES, "").orEmpty()
            .split(",")
            .filter { it.isNotBlank() }

    private fun keyFor(date: String) = "count_$date"

    companion object {
        private const val KEY_DATES = "dates"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
