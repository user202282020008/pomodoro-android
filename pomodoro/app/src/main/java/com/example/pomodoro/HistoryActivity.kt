package com.example.pomodoro

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.pomodoro.databinding.ActivityHistoryBinding
import com.example.pomodoro.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 7-day chart, all-time total, full per-day record, and CSV export. */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let { writeCsv(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnExport.setOnClickListener { exportLauncher.launch("pomodoro-records.csv") }

        val stats = PomodoroStats(this)
        binding.tvTotal.text = getString(R.string.history_total, stats.totalAllTime())
        binding.chart.setData(lastSevenDays(stats))

        val history = stats.history()
        if (history.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.btnExport.visibility = View.GONE
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

    private fun writeCsv(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { os ->
                val sb = StringBuilder("﻿") // UTF-8 BOM so Excel reads Chinese correctly
                sb.append(getString(R.string.csv_header)).append('\n')
                PomodoroStats(this).history().sortedBy { it.first }.forEach { (date, count) ->
                    sb.append(date).append(',').append(count).append('\n')
                }
                os.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(this, R.string.export_done, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.export_fail, Toast.LENGTH_SHORT).show()
        }
    }
}
