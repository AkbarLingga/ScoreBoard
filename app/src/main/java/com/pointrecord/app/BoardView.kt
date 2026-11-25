package com.pointrecord.app

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Insets
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.max

interface BoardActions {
    fun openCalc(team: Int)
    fun editTeam(team: Int)
    fun winSet(team: Int)
    fun removeSet(team: Int)
    fun toggleTimer()
    fun resetTimer()
    fun toggleMenu()
    fun newMatch()
    fun undo()
    fun swap()
    fun history()
    fun whistle()
    fun settings()
}

/** Setengah layar milik satu kubu. */
@SuppressLint("ViewConstructor")
class TeamView(context: Context, private val index: Int, actions: BoardActions) : FrameLayout(context) {
    private val content = Ui.column(context).apply { gravity = Gravity.CENTER_HORIZONTAL }
    private val emoji = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 46f)
        gravity = Gravity.CENTER
        includeFontPadding = false
        setTextColor(Ui.INK)
    }
    private val name = Ui.oneLine(Ui.text(context, "", 30f, bold = true)).apply {
        gravity = Gravity.CENTER
        setAutoSizeTextTypeUniformWithConfiguration(16, 34, 1, TypedValue.COMPLEX_UNIT_DIP)
        ellipsize = TextUtils.TruncateAt.END
    }
    private val badge = Ui.text(context, "MENANG", 15f, bold = true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(5), dp(16), dp(5))
        compoundDrawablePadding = dp(4)
        visibility = GONE
    }
    private val score = FitNumberView(context)
    private val edit = Ui.icon(context, R.drawable.ic_edit, "Edit kubu") { actions.editTeam(index) }
    private val calc = Ui.icon(context, R.drawable.ic_calc, "Buka kalkulator") { actions.openCalc(index) }

    init {
        content.addView(emoji, context.lp(WRAP, WRAP))
        content.addView(name, context.lp(MATCH, context.dp(44)))
        content.addView(badge, context.lp(WRAP, WRAP, t = 4))
        content.addView(score, context.lp(MATCH, 0, 1f, t = 8, b = 4))
        addView(content, LayoutParams(MATCH, MATCH))
        addView(edit, LayoutParams(dp(52), dp(52)))
        addView(calc, LayoutParams(dp(52), dp(52)))
        score.isClickable = true
        score.isFocusable = true
        score.setOnClickListener { actions.openCalc(index) }
        name.setOnClickListener { actions.editTeam(index) }
        emoji.setOnClickListener { actions.editTeam(index) }
    }

    fun bind(team: Team, winner: Boolean) {
        val fg = Ui.on(team.color)
        setBackgroundColor(team.color)
        emoji.text = team.emoji
        emoji.visibility = if (team.emoji.isEmpty()) GONE else VISIBLE
        name.text = team.name
        name.setTextColor(fg)
        if (score.number != team.score) score.bump()
        score.number = team.score
        score.color = fg
        score.background = Ui.ripple(null, dp(28).toFloat(), fg)
        score.contentDescription = "Poin ${team.name}: ${team.score}. Ketuk untuk menambah"
        for (b in listOf(edit, calc)) {
            b.imageTintList = ColorStateList.valueOf(fg)
            b.background = Ui.ripple(null, dp(26).toFloat(), fg)
        }
        badge.background = Ui.shape(fg, dp(20).toFloat())
        val onBadge = if (fg == Ui.WHITE) Ui.onWhite(team.color) else Ui.WHITE
        badge.setTextColor(onBadge)
        val star = context.getDrawable(R.drawable.ic_star)!!.mutate().apply {
            setTint(onBadge)
            setBounds(0, 0, dp(16), dp(16))
        }
        badge.setCompoundDrawables(star, null, null, null)
        val show = if (winner) VISIBLE else GONE
        if (badge.visibility != show) {
            badge.visibility = show
            if (winner) {
                badge.scaleX = 0.4f
                badge.scaleY = 0.4f
                badge.animate().scaleX(1f).scaleY(1f).setDuration(260).start()
            }
        }
    }

    /** Padding isi dan posisi tombol pojok, semua dalam px. */
    fun arrange(landscape: Boolean, left: Int, top: Int, right: Int, bottom: Int, edge: Insets) {
        content.setPadding(left, top, right, bottom)
        val m = dp(8)
        val side = if (landscape && index == 0) Gravity.START else Gravity.END
        fun place(v: View, vertical: Int) {
            val p = v.layoutParams as LayoutParams
            p.gravity = side or vertical
            p.setMargins(m + edge.left, m + edge.top, m + edge.right, m + edge.bottom)
        }
        place(edit, Gravity.TOP)
        place(calc, Gravity.BOTTOM)
        content.requestLayout()
        edit.requestLayout()
        calc.requestLayout()
    }
}

/** Angka set kedua kubu, dengan keterangan SET di tengah. */
@SuppressLint("ViewConstructor")
class SetsView(context: Context, actions: BoardActions) : LinearLayout(context) {
    private val digits = List(2) { i ->
        FitNumberView(context, minDigits = 1).apply {
            setPadding(dp(6), dp(14), dp(6), dp(14))
            background = Ui.ripple(null, dp(24).toFloat())
            setOnClickListener { actions.winSet(i) }
            setOnLongClickListener { actions.removeSet(i); true }
        }
    }
    private val pulses = digits.map {
        ObjectAnimator.ofPropertyValuesHolder(
            it,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.18f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.18f),
        ).apply {
            duration = 450
            repeatCount = 7 // genap bolak-balik, berakhir di ukuran normal
            repeatMode = ObjectAnimator.REVERSE
        }
    }

    private var lastLeader: Int? = null

    init {
        gravity = Gravity.CENTER_VERTICAL
        addView(digits[0], LayoutParams(dp(54), MATCH))
        addView(Ui.text(context, "SET", 12f, Ui.MUTED, bold = true), LayoutParams(WRAP, WRAP))
        addView(digits[1], LayoutParams(dp(54), MATCH))
    }

    fun bind(state: MatchState, leader: Int?) {
        state.teams.forEachIndexed { i, team ->
            val d = digits[i]
            if (d.number != team.sets) d.bump()
            d.number = team.sets
            d.color = Ui.onWhite(team.color)
            d.contentDescription = "Set ${team.name}: ${team.sets}. Ketuk untuk menang set, tahan untuk mengurangi"
            if (leader != lastLeader) {
                pulses[i].cancel()
                d.scaleX = 1f
                d.scaleY = 1f
                if (leader == i) pulses[i].start()
            }
        }
        lastLeader = leader
    }
}

/** Papan lengkap; menata ulang sendiri untuk landscape dan potrait. */
@SuppressLint("ViewConstructor")
class BoardView(context: Context, actions: BoardActions) : ViewGroup(context) {
    private val teams = List(2) { TeamView(context, it, actions) }
    private val line = View(context).apply { setBackgroundColor(Ui.WHITE) }
    private val playButton = Ui.icon(context, R.drawable.ic_play, "Mulai timer", pad = 11) { actions.toggleTimer() }
    private val resetButton = Ui.icon(context, R.drawable.ic_reset, "Reset timer", pad = 11) { actions.resetTimer() }
    private val timeText = Ui.text(context, "00:00", 30f, bold = true).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28f)
        fontFeatureSettings = "tnum"
        gravity = Gravity.CENTER
        maxLines = 1
    }
    private val timer = Ui.row(context).apply {
        gravity = Gravity.CENTER
        addView(playButton, context.lp(dp(46), dp(48)))
        addView(timeText, context.lp(WRAP, WRAP))
        addView(resetButton, context.lp(dp(46), dp(48)))
    }
    private val sets = SetsView(context, actions)
    private val handle = Ui.icon(context, R.drawable.ic_chevron, "Buka atau tutup menu", pad = 10) { actions.toggleMenu() }
    private val undoItem: View
    private val menu = GridPanel(context, cellHeight = dp(58), columns = 3, cellWidth = dp(66))

    private var landscape = true
    private var menuOpen = true
    private var safe: Insets = Insets.NONE

    init {
        fun item(icon: Int, label: String, onClick: () -> Unit): View {
            val cell = Ui.column(context).apply {
                gravity = Gravity.CENTER
                background = Ui.ripple(null, dp(16).toFloat())
                contentDescription = label
                isFocusable = true
                setOnClickListener { onClick() }
            }
            cell.addView(ImageView(context).apply {
                setImageResource(icon)
                imageTintList = ColorStateList.valueOf(Ui.INK)
            }, context.lp(dp(26), dp(26)))
            cell.addView(Ui.text(context, label, 12f, Ui.MUTED).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
                maxLines = 1
            }, context.lp(WRAP, WRAP, t = 4))
            menu.addView(cell)
            return cell
        }
        item(R.drawable.ic_add, "Match baru") { actions.newMatch() }
        undoItem = item(R.drawable.ic_undo, "Undo") { actions.undo() }
        item(R.drawable.ic_swap, "Tukar sisi") { actions.swap() }
        item(R.drawable.ic_history, "Riwayat") { actions.history() }
        item(R.drawable.ic_whistle, "Peluit") { actions.whistle() }
        item(R.drawable.ic_settings, "Atur") { actions.settings() }
        teams.forEach { addView(it) }
        listOf(line, timer, sets, menu, handle).forEach { addView(it) }
    }

    fun bind(match: Match) {
        val leader = match.leader()
        teams.forEachIndexed { i, v -> v.bind(match.state.teams[i], leader == i) }
        sets.bind(match.state, leader)
        undoItem.isEnabled = match.canUndo
        undoItem.alpha = if (match.canUndo) 1f else 0.35f
    }

    fun bindTimer(text: String, running: Boolean) {
        if (timeText.text.toString() != text) timeText.text = text
        playButton.setImageResource(if (running) R.drawable.ic_pause else R.drawable.ic_play)
        playButton.contentDescription = if (running) "Jeda timer" else "Mulai timer"
    }

    fun setMenuOpen(open: Boolean) {
        menuOpen = open
        handle.setImageResource(if (open) R.drawable.ic_chevron else R.drawable.ic_chevron_up)
        requestLayout()
    }

    fun setSafeArea(i: Insets) {
        if (i == safe) return
        safe = i
        requestLayout()
    }

    private fun exactly(v: Int) = MeasureSpec.makeMeasureSpec(v.coerceAtLeast(0), MeasureSpec.EXACTLY)
    private fun atMost(v: Int) = MeasureSpec.makeMeasureSpec(v.coerceAtLeast(0), MeasureSpec.AT_MOST)

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        val h = MeasureSpec.getSize(hSpec)
        landscape = w > h
        menu.visibility = if (menuOpen) VISIBLE else GONE
        if (landscape) measureLandscape(w, h) else measurePortrait(w, h)
        setMeasuredDimension(w, h)
    }

    private var styled: Boolean? = null

    /** Tampilan komponen tengah; hanya diubah saat orientasi berganti. */
    private fun style(land: Boolean) {
        if (styled == land) return
        styled = land
        val r = dp(26).toFloat()
        val h = dp(22).toFloat()
        val side = if (land) dp(8) else 0
        timer.background = if (land) Ui.shape(Ui.WHITE, 0f, 0f, r, r) else null
        timer.setPadding(side, 0, side, 0)
        timeText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, if (land) 28f else 24f)
        for (b in listOf(playButton, resetButton)) {
            b.layoutParams.width = dp(if (land) 46 else 42)
            b.requestLayout()
        }
        sets.background = if (land) Ui.shape(Ui.WHITE, r) else null
        menu.background = if (land) Ui.shape(Ui.WHITE, r, r, 0f, 0f) else null
        menu.columns = if (land) 3 else 6
        if (land) menu.setPadding(dp(10), dp(6), dp(10), dp(8)) else menu.setPadding(dp(6), 0, dp(6), dp(6))
        handle.background = Ui.ripple(if (land) Ui.shape(Ui.WHITE, h, h, 0f, 0f) else null, h)
    }

    private fun measureLandscape(w: Int, h: Int) {
        style(true)
        timer.measure(atMost(w / 2), exactly(dp(54)))
        sets.measure(atMost(w / 2), exactly(dp(64)))
        menu.measure(atMost(w / 2), atMost(h))
        handle.measure(exactly(dp(60)), exactly(dp(34)))
        line.measure(exactly(dp(5)), exactly(h))

        val side = max(safe.left, safe.right)
        val inner = max(max(timer.measuredWidth, sets.measuredWidth), dp(218)) / 2 + dp(8)
        val outer = side + dp(28)
        val top = safe.top + dp(20)
        val bottom = safe.bottom + dp(20)
        teams[0].arrange(true, outer, top, inner, bottom, Insets.of(side, safe.top, 0, safe.bottom))
        teams[1].arrange(true, inner, top, outer, bottom, Insets.of(0, safe.top, side, safe.bottom))
        teams[0].measure(exactly(w / 2), exactly(h))
        teams[1].measure(exactly(w - w / 2), exactly(h))
    }

    private fun measurePortrait(w: Int, h: Int) {
        style(false)
        timer.measure(atMost(w), exactly(dp(64)))
        sets.measure(atMost(w), exactly(dp(64)))
        handle.measure(exactly(dp(46)), exactly(dp(48)))
        menu.measure(exactly(w), atMost(h))
        val belt = beltHeight()
        line.measure(exactly(w), exactly(belt))

        val teamH = (h - belt) / 2
        val pad = dp(52)
        teams[0].arrange(false, pad, safe.top + dp(14), pad, dp(12), Insets.of(0, safe.top, 0, 0))
        teams[1].arrange(false, pad, dp(16), pad, safe.bottom + dp(16), Insets.of(0, 0, 0, safe.bottom))
        teams[0].measure(exactly(w), exactly(teamH))
        teams[1].measure(exactly(w), exactly(h - belt - teamH))
    }

    private fun beltHeight() = dp(68) + if (menuOpen) menu.measuredHeight else 0

    private fun View.at(x: Int, y: Int) = layout(x, y, x + measuredWidth, y + measuredHeight)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l
        val h = b - t
        if (landscape) {
            val cx = w / 2
            teams[0].at(0, 0)
            teams[1].at(w / 2, 0)
            line.at(cx - line.measuredWidth / 2, 0)
            timer.at(cx - timer.measuredWidth / 2, safe.top)
            sets.at(cx - sets.measuredWidth / 2, safe.top + timer.measuredHeight + dp(10))
            val menuTop = if (menuOpen) h - safe.bottom - menu.measuredHeight else h - safe.bottom
            if (menuOpen) menu.at(cx - menu.measuredWidth / 2, menuTop)
            handle.at(cx - handle.measuredWidth / 2, menuTop - handle.measuredHeight)
        } else {
            val top = teams[0].measuredHeight
            val belt = beltHeight()
            teams[0].at(0, 0)
            line.at(0, top)
            teams[1].at(0, top + belt)
            val mid = top + dp(34)
            timer.at(dp(4), mid - timer.measuredHeight / 2)
            val free = w - dp(4) - handle.measuredWidth
            val x = (dp(4) + timer.measuredWidth + free - sets.measuredWidth) / 2
            sets.at(x, mid - sets.measuredHeight / 2)
            handle.at(free, mid - handle.measuredHeight / 2)
            if (menuOpen) menu.at(0, top + dp(68))
        }
    }
}
