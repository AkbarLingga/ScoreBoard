package com.pointrecord.app

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import kotlin.math.max
import kotlin.math.min

/** Angka yang selalu dibesarkan sampai memenuhi ruangnya. */
@SuppressLint("ViewConstructor")
class FitNumberView(context: Context, private val minDigits: Int = 2) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Ui.bold(context)
        textAlign = Paint.Align.CENTER
        fontFeatureSettings = "tnum"
    }
    private val bounds = Rect()

    var color: Int = Ui.WHITE
        set(v) { field = v; paint.color = v; invalidate() }

    var number: Int = 0
        set(v) {
            if (field == v) return
            field = v
            invalidate()
        }

    fun bump() {
        animate().cancel()
        scaleX = 1.1f
        scaleY = 1.1f
        animate().scaleX(1f).scaleY(1f).setDuration(220).start()
    }

    override fun onDraw(canvas: Canvas) {
        val w = (width - paddingLeft - paddingRight).toFloat()
        val h = (height - paddingTop - paddingBottom).toFloat()
        if (w <= 0 || h <= 0) return
        // Minus asli (U+2212) sejajar dengan angka.
        val s = if (number < 0) "−${-number}" else number.toString()
        paint.textSize = 100f
        paint.getTextBounds("0", 0, 1, bounds)
        // Minimal selebar [minDigits] supaya ukuran tidak loncat saat digit bertambah.
        val wide = max(paint.measureText(s), paint.measureText("0".repeat(minDigits)))
        paint.textSize = 100f * min(w / wide, h / bounds.height())
        paint.getTextBounds("0", 0, 1, bounds)
        canvas.drawText(
            s, paddingLeft + w / 2, paddingTop + h / 2 - bounds.exactCenterY(), paint
        )
    }
}

/** Grid sederhana: [columns] tetap, atau dihitung dari lebar sel minimal. */
@SuppressLint("ViewConstructor")
class GridPanel(
    context: Context,
    private val cellHeight: Int,
    var columns: Int = 0,
    private val minCell: Int = 0,
    private val gap: Int = 0,
    /** Lebar tetap per sel kalau lebar panel tidak ditentukan. */
    private val cellWidth: Int = 0,
) : ViewGroup(context) {
    /** Isi tinggi yang tersedia (untuk keypad). */
    var fillHeight = false

    private fun cols(w: Int) = if (columns > 0) columns else max(1, (w + gap) / (minCell + gap))
    private fun rows(c: Int) = (childCount + c - 1) / c
    private fun inner(w: Int) = w - paddingLeft - paddingRight

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = if (MeasureSpec.getMode(wSpec) == MeasureSpec.EXACTLY || cellWidth == 0) MeasureSpec.getSize(wSpec)
        else columns * cellWidth + (columns - 1) * gap + paddingLeft + paddingRight
        val c = cols(inner(w))
        val r = rows(c)
        val vPad = paddingTop + paddingBottom
        val cw = (inner(w) - gap * (c - 1)) / c
        var h = r * cellHeight + (r - 1).coerceAtLeast(0) * gap + vPad
        if (fillHeight && MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY) h = MeasureSpec.getSize(hSpec)
        val ch = if (r == 0) 0 else (h - vPad - gap * (r - 1)) / r
        for (i in 0 until childCount) getChildAt(i).measure(
            MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY),
        )
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val c = cols(inner(r - l))
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            val x = paddingLeft + (i % c) * (v.measuredWidth + gap)
            val y = paddingTop + (i / c) * (v.measuredHeight + gap)
            v.layout(x, y, x + v.measuredWidth, y + v.measuredHeight)
        }
    }
}
