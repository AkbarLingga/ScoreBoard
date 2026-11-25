package com.pointrecord.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.pow

const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

object Ui {
    val INK = Color.parseColor("#23262F")
    val MUTED = Color.parseColor("#626877")
    val LINE = Color.parseColor("#DFE3EB")
    val SOFT = Color.parseColor("#EFF2F7")
    const val WHITE = Color.WHITE

    val COLORS = listOf(
        "#1E88F0", "#EF3E36", "#159957", "#F59A0B", "#7A4FE0", "#E0407F",
        "#0E9AA7", "#8A5A36", "#1F3A7A", "#A3202F", "#5C7F12", "#F2C515",
        "#3A3F4B", "#8D95A5", "#5BB8F5", "#F58A6C",
    ).map { Color.parseColor(it) }

    val EMOJIS = listOf(
        "🐯", "🦅", "🦁", "🐲", "🐮", "🐸", "🐺", "🦈", "🐼", "🐵", "🦊", "🐔",
        "🔥", "⚡", "⭐", "👑", "💎", "🚀", "🏸", "🏐", "⚽", "🏓", "🎯", "😎", "",
    )

    private var bold: Typeface? = null
    private var semi: Typeface? = null
    fun bold(c: Context): Typeface = bold ?: c.resources.getFont(R.font.saira_extrabold).also { bold = it }
    fun semi(c: Context): Typeface = semi ?: c.resources.getFont(R.font.saira_semibold).also { semi = it }

    private fun luminance(c: Int): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(Color.red(c)) + 0.7152 * ch(Color.green(c)) + 0.0722 * ch(Color.blue(c))
    }

    /** Warna tulisan yang terbaca di atas [bg]. */
    fun on(bg: Int): Int = if (luminance(bg) > 0.42) INK else WHITE

    /** [c] digelapkan secukupnya supaya terbaca di atas putih. */
    fun onWhite(c: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(c, hsv)
        var out = c
        while (1.05 / (luminance(out) + 0.05) < 3.0 && hsv[2] > 0.1f) {
            hsv[2] -= 0.05f
            out = Color.HSVToColor(hsv)
        }
        return out
    }

    fun alpha(c: Int, a: Float): Int = Color.argb((a * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c))

    fun shape(color: Int, radius: Float, stroke: Int = 0, strokeColor: Int = 0) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (stroke > 0) setStroke(stroke, strokeColor)
    }

    /** Sudut: kiri-atas, kanan-atas, kanan-bawah, kiri-bawah. */
    fun shape(color: Int, tl: Float, tr: Float, br: Float, bl: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadii = floatArrayOf(tl, tl, tr, tr, br, br, bl, bl)
    }

    fun ripple(content: Drawable?, radius: Float, tint: Int = INK): Drawable =
        RippleDrawable(ColorStateList.valueOf(alpha(tint, 0.18f)), content, shape(WHITE, radius))

    fun text(c: Context, s: String, sp: Float, color: Int = INK, bold: Boolean = false) = TextView(c).apply {
        text = s
        textSize = sp
        setTextColor(color)
        typeface = if (bold) bold(c) else semi(c)
        includeFontPadding = false
    }

    fun oneLine(t: TextView) = t.apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }

    fun icon(c: Context, res: Int, label: String, tint: Int = INK, pad: Int = 12, onClick: (View) -> Unit) =
        ImageView(c).apply {
            setImageResource(res)
            imageTintList = ColorStateList.valueOf(tint)
            contentDescription = label
            scaleType = ImageView.ScaleType.FIT_CENTER
            val p = c.dp(pad)
            setPadding(p, p, p, p)
            background = ripple(null, c.dp(24).toFloat(), tint)
            setOnClickListener(onClick)
        }

    fun button(c: Context, label: String, bg: Int, fg: Int, onClick: () -> Unit) =
        text(c, label, 16f, fg, bold = true).apply {
            gravity = Gravity.CENTER
            minHeight = c.dp(52)
            setPadding(c.dp(16), c.dp(8), c.dp(16), c.dp(8))
            val r = c.dp(16).toFloat()
            background = ripple(shape(bg, r), r, fg)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }

    fun column(c: Context) = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
    fun row(c: Context) = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun View.dp(v: Int): Int = context.dp(v)

/** LinearLayout.LayoutParams singkat; margin dalam dp. */
fun Context.lp(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
    LinearLayout.LayoutParams(w, h, weight).apply { setMargins(dp(l), dp(t), dp(r), dp(b)) }
