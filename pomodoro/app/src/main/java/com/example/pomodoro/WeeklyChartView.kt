package com.example.pomodoro

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.max

/** Minimal line chart of the last few days' completed-focus counts (white on transparent). */
class WeeklyChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data: List<Pair<String, Int>> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = dp(2.5f)
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55FFFFFF
        strokeWidth = dp(1f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(12f)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun setData(values: List<Pair<String, Int>>) {
        data = values
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val leftPad = dp(10f)
        val rightPad = dp(10f)
        val topPad = dp(22f)     // room for value labels
        val bottomPad = dp(24f)  // room for day labels

        val chartW = width - leftPad - rightPad
        val chartH = height - topPad - bottomPad
        val baseY = topPad + chartH
        val maxVal = max(1, data.maxOf { it.second })
        val n = data.size
        val stepX = if (n > 1) chartW / (n - 1) else 0f

        canvas.drawLine(leftPad, baseY, leftPad + chartW, baseY, baselinePaint)

        val points = data.mapIndexed { i, (_, v) ->
            val x = leftPad + stepX * i
            val y = baseY - (v.toFloat() / maxVal) * chartH
            x to y
        }

        val fill = Path().apply {
            moveTo(points.first().first, baseY)
            points.forEach { lineTo(it.first, it.second) }
            lineTo(points.last().first, baseY)
            close()
        }
        canvas.drawPath(fill, fillPaint)

        val line = Path().apply {
            moveTo(points.first().first, points.first().second)
            points.drop(1).forEach { lineTo(it.first, it.second) }
        }
        canvas.drawPath(line, linePaint)

        data.forEachIndexed { i, (label, v) ->
            val (x, y) = points[i]
            canvas.drawCircle(x, y, dp(3f), dotPaint)
            if (v > 0) canvas.drawText(v.toString(), x, y - dp(8f), valuePaint)
            canvas.drawText(label, x, baseY + dp(18f), labelPaint)
        }
    }

    private fun dp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)

    private fun sp(v: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
}
