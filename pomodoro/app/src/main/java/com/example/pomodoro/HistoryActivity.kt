package com.example.pomodoro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.pomodoro.databinding.ActivityHistoryBinding
import com.example.pomodoro.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Shows a 7-day chart, the all-time total, and the full per-day record. */
class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val stats = PomodoroStats(this)
        binding.tvTotal.text = getString(R.string.history_total, stats.totalAllTime())
        binding.chart.setData(lastSevenDays(stats))

        val history = stats.history()
        if (history.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            val inflater = LayoutInflater.from(this)
            for ((date, count) in history) {
                val row = ItemHistoryBinding.inflate(inflater, binding.container, false)
                row.tvDate.text = date
                row.tvCount.text = getString(R.string.history_count, count)
                binding.container.addView(row.root)
            }
        }
    }

    /** Last 7 calendar days ending today, labelled M/d, value = that day's count. */
    private fun lastSevenDays(stats: PomodoroStats): List<Pair<String, Int>> {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("M/d", Locale.US)
        return (6 downTo 0).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            labelFmt.format(cal.time) to stats.getCount(keyFmt.format(cal.time))
        }
    }
}
