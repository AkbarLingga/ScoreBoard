package com.pointrecord.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

/** Panel di atas papan. [build] dipanggil ulang saat HP diputar; datanya hidup di luar build. */
class Sheet(val widthDp: (landscape: Boolean) -> Int, val build: (landscape: Boolean) -> View)

/** Bagian panel yang disembunyikan saat keyboard memakan layar landscape. */
const val HIDE_ON_IME = "hideOnIme"

class Prefs(var orientation: Int, var vibrate: Boolean)

class Sheets(private val c: Context, private val close: () -> Unit) {
    private fun dp(v: Int) = c.dp(v)

    private fun EditText.onText(f: (String) -> Unit) = addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) = Unit
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) = Unit
        override fun afterTextChanged(s: Editable) = f(s.toString())
    })

    private fun field(value: String, hint: String, label: String) = EditText(c).apply {
        setText(value)
        this.hint = hint
        contentDescription = label
        typeface = Ui.semi(c)
        textSize = 18f
        setTextColor(Ui.INK)
        setHintTextColor(Ui.MUTED)
        isSingleLine = true
        imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        background = Ui.shape(Ui.SOFT, dp(14).toFloat())
        setPadding(dp(16), 0, dp(16), 0)
        setSelectAllOnFocus(true)
    }

    private fun heading(s: String) = Ui.text(c, s, 14f, Ui.MUTED)

    private fun header(title: View) = Ui.row(c).apply {
        addView(title, c.lp(0, WRAP, 1f, r = 8))
        addView(Ui.icon(c, R.drawable.ic_close, "Tutup", pad = 12) { close() }.apply { translationX = dp(10).toFloat() }, c.lp(dp(48), dp(48)))
    }

    private fun footer(left: View, right: View) = Ui.row(c).apply {
        addView(left, c.lp(0, WRAP, 1f, r = 5))
        addView(right, c.lp(0, WRAP, 1f, l = 5))
    }

    private fun cancel() = Ui.button(c, "Batal", Ui.SOFT, Ui.INK) { close() }

    /** Judul dan tombol tetap, isi bisa digulir. */
    private fun frame(title: String, body: View, footer: View?) =
        frame(Ui.text(c, title, 20f, bold = true), body, footer)

    private fun frame(title: View, body: View, footer: View?) = Ui.column(c).apply {
        setPadding(dp(20), dp(10), dp(20), dp(18))
        addView(header(title).apply { tag = HIDE_ON_IME }, c.lp(MATCH, WRAP))
        addView(ScrollView(c).apply {
            isVerticalFadingEdgeEnabled = true
            addView(body)
        }, c.lp(MATCH, WRAP, 1f, t = 4))
        if (footer != null) addView(footer.apply { tag = HIDE_ON_IME }, c.lp(MATCH, WRAP, t = 14))
    }

    // ---------------------------------------------------------------- kalkulator

    fun calculator(team: Team, onAdd: (Long) -> Unit): Sheet {
        val calc = Calc()
        return Sheet({ if (it) 660 else 400 }) { landscape -> calculatorView(team, calc, landscape, onAdd) }
    }

    private fun calculatorView(team: Team, calc: Calc, landscape: Boolean, onAdd: (Long) -> Unit): View {
        val expr = Ui.text(c, "", 40f, bold = true).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            maxLines = 1
            fontFeatureSettings = "tnum"
            setAutoSizeTextTypeUniformWithConfiguration(18, 42, 1, android.util.TypedValue.COMPLEX_UNIT_DIP)
        }
        val result = Ui.text(c, "", 18f, Ui.MUTED).apply { gravity = Gravity.END }
        fun commit(sign: Int) {
            val v = calc.value() ?: return
            if (v != 0L) {
                close()
                onAdd(sign * v)
            }
        }
        val accent = Ui.onWhite(team.color)
        val minus = Ui.button(c, "", Ui.SOFT, Ui.INK) { commit(-1) }
        val plus = Ui.button(c, "", accent, Ui.WHITE) { commit(1) }

        fun refresh() {
            val v = calc.value()
            expr.text = calc.text.ifEmpty { "0" }.replace("*", " × ").replace("+", " + ")
                .replace(Regex("(?<=\\d)-"), " − ").replace("-", "−")
            expr.setTextColor(if (calc.text.isEmpty()) Ui.MUTED else Ui.INK)
            val compound = calc.text.drop(1).any { !it.isDigit() }
            result.text = if (compound && v != null) "= ${signed(v)}" else " "
            val ready = v != null && v != 0L
            fun target(sign: Int) = signed((team.score + sign * (v ?: 0)).coerceIn(Match.MIN_SCORE, Match.MAX_SCORE))
            plus.text = if (ready) "Tambah → ${target(1)}" else "Tambah"
            minus.text = if (ready) "Kurangi → ${target(-1)}" else "Kurangi"
            for (b in listOf(plus, minus)) {
                b.isEnabled = ready
                b.alpha = if (ready) 1f else 0.45f
            }
        }

        val head = Ui.row(c).apply {
            if (team.emoji.isNotEmpty()) addView(Ui.text(c, team.emoji, 26f), c.lp(WRAP, WRAP, r = 10))
            addView(Ui.column(c).apply {
                addView(Ui.oneLine(Ui.text(c, team.name, 18f, accent, bold = true)), c.lp(MATCH, WRAP))
                addView(Ui.text(c, "Poin sekarang ${signed(team.score.toLong())}", 14f, Ui.MUTED), c.lp(MATCH, WRAP, t = 2))
            }, c.lp(0, WRAP, 1f))
            addView(Ui.icon(c, R.drawable.ic_close, "Tutup", pad = 12) { close() }, c.lp(dp(48), dp(48)))
        }
        val display = Ui.column(c).apply {
            background = Ui.shape(Ui.SOFT, dp(18).toFloat())
            setPadding(dp(16), dp(6), dp(16), dp(6))
            gravity = Gravity.CENTER_VERTICAL
            addView(expr, c.lp(MATCH, dp(48)))
            addView(result, c.lp(MATCH, WRAP, t = 2))
        }
        val keys = GridPanel(c, cellHeight = dp(58), columns = 4, gap = dp(8)).apply {
            for (k in "789<456*123-C0=+") addView(key(k, accent) { calc.press(k); refresh() })
        }
        refresh()

        return if (landscape) Ui.row(c).apply {
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(Ui.column(c).apply {
                addView(head, c.lp(MATCH, WRAP))
                addView(display, c.lp(MATCH, 0, 1f, t = 8, b = 10))
                addView(plus, c.lp(MATCH, WRAP))
                addView(minus, c.lp(MATCH, WRAP, t = 8))
            }, c.lp(0, dp(290), 1f, r = 14))
            keys.fillHeight = true
            addView(keys, c.lp(0, dp(290), 1.15f))
        } else Ui.column(c).apply {
            setPadding(dp(18), dp(12), dp(18), dp(18))
            addView(head, c.lp(MATCH, WRAP))
            addView(display, c.lp(MATCH, WRAP, t = 10, b = 12))
            addView(keys, c.lp(MATCH, WRAP))
            addView(footer(minus, plus), c.lp(MATCH, WRAP, t = 14))
        }
    }

    private fun signed(v: Long) = if (v < 0) "−${-v}" else v.toString()

    private fun key(k: Char, accent: Int, onClick: () -> Unit): View {
        val r = c.dp(16).toFloat()
        val digit = k.isDigit()
        val bg = Ui.ripple(Ui.shape(if (digit) Ui.SOFT else Ui.alpha(accent, 0.13f), r), r)
        val label = when (k) {
            '*' -> "×"
            '-' -> "−"
            else -> k.toString()
        }
        val v: View = if (k == '<') ImageView(c).apply {
            setImageResource(R.drawable.ic_backspace)
            imageTintList = android.content.res.ColorStateList.valueOf(Ui.INK)
            scaleType = ImageView.ScaleType.CENTER
        } else Ui.text(c, label, 26f, Ui.INK, bold = true).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 25f)
            gravity = Gravity.CENTER
        }
        v.background = bg
        v.isFocusable = true
        v.contentDescription = when (k) {
            '<' -> "Hapus satu"
            'C' -> "Hapus semua"
            '*' -> "Kali"
            '-' -> "Kurang"
            '+' -> "Tambah angka"
            '=' -> "Sama dengan"
            else -> label
        }
        v.setOnClickListener { onClick() }
        return v
    }

    // ---------------------------------------------------------------- edit kubu

    fun editTeam(index: Int, team: Team, onSave: (String, Int, String) -> Unit): Sheet {
        var name = team.name
        var color = team.color
        var emoji = team.emoji
        return Sheet({ if (it) 680 else 420 }) { landscape ->
            val preview = Ui.oneLine(Ui.text(c, "", 22f, bold = true)).apply {
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(9), dp(16), dp(9))
            }
            fun refreshPreview() {
                preview.text = listOf(emoji, name.trim().ifEmpty { "Kubu ${index + 1}" })
                    .filter { it.isNotEmpty() }.joinToString("  ")
                preview.setTextColor(Ui.on(color))
                preview.background = Ui.shape(color, dp(18).toFloat())
            }

            val input = field(name, "Kubu ${index + 1}", "Nama kubu").apply {
                filters = arrayOf(InputFilter.LengthFilter(20))
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                onText { name = it; refreshPreview() }
            }

            val swatches = GridPanel(c, cellHeight = dp(46), columns = 8, gap = dp(2))
            val marks = mutableListOf<Pair<Int, ImageView>>()
            fun refreshColors() {
                marks.forEach { (value, v) -> v.setImageResource(if (value == color) R.drawable.ic_check else 0) }
                refreshPreview()
            }
            for (value in Ui.COLORS) {
                val dot = ImageView(c).apply {
                    scaleType = ImageView.ScaleType.CENTER
                    imageTintList = android.content.res.ColorStateList.valueOf(Ui.on(value))
                    background = Ui.shape(value, dp(20).toFloat())
                    contentDescription = "Warna ${Ui.COLORS.indexOf(value) + 1}"
                    isFocusable = true
                    setOnClickListener { color = value; refreshColors() }
                }
                marks += value to dot
                swatches.addView(FrameLayout(c).apply {
                    addView(dot, FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER))
                    setOnClickListener { dot.performClick() }
                })
            }
            val hue = SeekBar(c).apply {
                max = 359
                val hsv = FloatArray(3)
                Color.colorToHSV(color, hsv)
                progress = hsv[0].toInt()
                contentDescription = "Warna lain"
                progressDrawable = null
                splitTrack = false
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    IntArray(13) { Color.HSVToColor(floatArrayOf(it * 30f % 360, 0.8f, 0.88f)) }
                ).apply { cornerRadius = dp(10).toFloat() }
                thumb = Ui.shape(Ui.WHITE, dp(14).toFloat(), dp(3), Ui.INK).apply { setSize(dp(28), dp(28)) }
                thumbOffset = 0
                setPadding(0, 0, 0, 0)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                        if (!fromUser) return
                        color = Color.HSVToColor(floatArrayOf(p.toFloat(), 0.8f, 0.88f))
                        refreshColors()
                    }
                    override fun onStartTrackingTouch(s: SeekBar) = Unit
                    override fun onStopTrackingTouch(s: SeekBar) = Unit
                })
            }

            val emojis = GridPanel(c, cellHeight = dp(48), minCell = dp(44), gap = dp(2))
            val emojiCells = mutableListOf<Pair<String, View>>()
            fun refreshEmoji() {
                emojiCells.forEach { (e, v) ->
                    val r = dp(14).toFloat()
                    v.background = if (e == emoji) Ui.shape(Ui.SOFT, r, dp(2), Ui.INK) else Ui.ripple(null, r)
                }
                refreshPreview()
            }
            for (e in Ui.EMOJIS) {
                val cell = Ui.text(c, e.ifEmpty { "Tanpa" }, if (e.isEmpty()) 12f else 24f).apply {
                    gravity = Gravity.CENTER
                    isFocusable = true
                    setOnClickListener { emoji = e; refreshEmoji() }
                }
                emojiCells += e to cell
                emojis.addView(cell)
            }
            refreshColors()
            refreshEmoji()

            val first = Ui.column(c).apply {
                if (!landscape) addView(heading("Nama"), c.lp(MATCH, WRAP, t = 4, b = 6))
                addView(input, c.lp(MATCH, dp(48)))
                addView(heading("Warna"), c.lp(MATCH, WRAP, t = if (landscape) 8 else 14, b = 2))
                addView(swatches, c.lp(MATCH, WRAP))
                addView(heading("Warna lain"), c.lp(MATCH, WRAP, t = 8, b = 8))
                addView(hue, c.lp(MATCH, dp(28)))
            }
            val second = Ui.column(c).apply {
                addView(heading("Emoji"), c.lp(MATCH, WRAP, t = if (landscape) 0 else 16, b = 4))
                addView(emojis, c.lp(MATCH, WRAP))
            }
            val body = if (landscape) LinearLayout(c).apply {
                addView(first, c.lp(0, WRAP, 1.1f, r = 20))
                addView(second, c.lp(0, WRAP, 1f))
            } else Ui.column(c).apply {
                addView(first, c.lp(MATCH, WRAP))
                addView(second, c.lp(MATCH, WRAP))
            }
            val save = Ui.button(c, "Simpan", Ui.INK, Ui.WHITE) { close(); onSave(name, color, emoji) }
            frame(preview, body, footer(cancel(), save))
        }
    }

    // ---------------------------------------------------------------- pengaturan

    private fun chips(options: List<Pair<String, () -> Unit>>, selected: () -> Int): Pair<GridPanel, () -> Unit> {
        val grid = GridPanel(c, cellHeight = c.dp(48), minCell = c.dp(88), gap = c.dp(8))
        val views = mutableListOf<TextView>()
        val refresh = {
            views.forEachIndexed { i, v ->
                val on = i == selected()
                val r = c.dp(14).toFloat()
                v.background = Ui.ripple(Ui.shape(if (on) Ui.INK else Ui.SOFT, r), r, if (on) Ui.WHITE else Ui.INK)
                v.setTextColor(if (on) Ui.WHITE else Ui.INK)
                v.isSelected = on
            }
        }
        options.forEach { (label, action) ->
            val v = Ui.oneLine(Ui.text(c, label, 16f)).apply {
                gravity = Gravity.CENTER
                isFocusable = true
                setOnClickListener { action(); refresh() }
            }
            views += v
            grid.addView(v)
        }
        refresh()
        return grid to refresh
    }

    fun settings(maxPoint: Int, prefs: Prefs, onSave: (Int, Prefs) -> Unit): Sheet {
        var maxText = if (maxPoint > 0) maxPoint.toString() else ""
        val draft = Prefs(prefs.orientation, prefs.vibrate)
        return Sheet({ if (it) 620 else 420 }) { landscape ->
            val presets = listOf(0, 21, 25, 50, 100, 200)
            val input = field(maxText, "Tanpa batas", "Max poin").apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(4))
            }
            val (maxChips, refreshMax) = chips(
                presets.map { p ->
                    (if (p == 0) "Mati" else p.toString()) to {
                        input.setText(if (p == 0) "" else p.toString())
                        input.setSelection(input.text.length)
                    }
                }
            ) { presets.indexOf(maxText.toIntOrNull() ?: 0) }
            input.onText { maxText = it; refreshMax() }
            val (orientation, _) = chips(
                listOf("Otomatis", "Landscape", "Potrait").mapIndexed { i, s -> s to { draft.orientation = i } }
            ) { draft.orientation }
            val (vibrate, _) = chips(
                listOf("Nyala" to { draft.vibrate = true }, "Mati" to { draft.vibrate = false })
            ) { if (draft.vibrate) 0 else 1 }

            val first = Ui.column(c).apply {
                addView(heading("Max poin per set"), c.lp(MATCH, WRAP, b = 6))
                addView(input, c.lp(MATCH, dp(50)))
                addView(maxChips, c.lp(MATCH, WRAP, t = 8))
            }
            val second = Ui.column(c).apply {
                addView(heading("Posisi layar"), c.lp(MATCH, WRAP, t = if (landscape) 0 else 18, b = 6))
                addView(orientation, c.lp(MATCH, WRAP))
                addView(heading("Getar saat max poin tercapai"), c.lp(MATCH, WRAP, t = 18, b = 6))
                addView(vibrate, c.lp(MATCH, WRAP))
            }
            val body = if (landscape) LinearLayout(c).apply {
                addView(first, c.lp(0, WRAP, 1f, r = 20))
                addView(second, c.lp(0, WRAP, 1f))
            } else Ui.column(c).apply {
                addView(first, c.lp(MATCH, WRAP))
                addView(second, c.lp(MATCH, WRAP))
            }
            val save = Ui.button(c, "Simpan", Ui.INK, Ui.WHITE) {
                close()
                onSave(maxText.toIntOrNull() ?: 0, draft)
            }
            frame("Pengaturan", body, footer(cancel(), save))
        }
    }

    // ---------------------------------------------------------------- riwayat & konfirmasi

    fun history(state: MatchState): Sheet = Sheet({ 420 }) {
        val (a, b) = state.teams
        val body = Ui.column(c)
        fun line(left: String, mid: String, right: String, win: Int?, bold: Boolean) = Ui.row(c).apply {
            setPadding(0, dp(10), 0, dp(10))
            fun cell(s: String, g: Int, color: Int, strong: Boolean) =
                Ui.oneLine(Ui.text(c, s, if (bold) 16f else 22f, color, strong)).apply { gravity = g }
            addView(cell(left, Gravity.END, Ui.onWhite(a.color), bold || win == 0).apply { alpha = if (win == 1) 0.55f else 1f }, c.lp(0, WRAP, 1f))
            addView(Ui.text(c, mid, 14f, Ui.MUTED).apply { gravity = Gravity.CENTER }, c.lp(dp(72), WRAP))
            addView(cell(right, Gravity.START, Ui.onWhite(b.color), bold || win == 1).apply { alpha = if (win == 0) 0.55f else 1f }, c.lp(0, WRAP, 1f))
        }
        if (state.history.isEmpty()) {
            body.addView(
                Ui.text(c, "Belum ada set yang selesai. Ketuk angka SET di papan untuk mencatat pemenang set.", 16f, Ui.MUTED)
                    .apply { setLineSpacing(0f, 1.25f) },
                c.lp(MATCH, WRAP, t = 4, b = 12)
            )
        } else {
            body.addView(line(a.name, "${a.sets} - ${b.sets}", b.name, null, true))
            state.history.forEachIndexed { i, r ->
                body.addView(View(c).apply { setBackgroundColor(Ui.LINE) }, c.lp(MATCH, dp(1)))
                body.addView(line(signed(r.a.toLong()), "Set ${i + 1}", signed(r.b.toLong()), r.winner, false))
            }
        }
        frame("Riwayat set", body, null)
    }

    fun confirm(title: String, message: String, yes: String, onYes: () -> Unit): Sheet = Sheet({ 400 }) {
        val body = Ui.text(c, message, 16f, Ui.MUTED).apply { setLineSpacing(0f, 1.25f) }
        frame(title, body, footer(cancel(), Ui.button(c, yes, Ui.INK, Ui.WHITE) { close(); onYes() }))
    }
}
