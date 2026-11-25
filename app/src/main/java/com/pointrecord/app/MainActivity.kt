package com.pointrecord.app

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Insets
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity(), BoardActions {
    private lateinit var store: SharedPreferences
    private lateinit var match: Match
    private lateinit var timer: GameTimer
    private lateinit var prefs: Prefs
    private lateinit var root: FrameLayout
    private lateinit var board: BoardView
    private lateinit var overlay: FrameLayout
    private lateinit var snackbar: FrameLayout

    private val handler = Handler(Looper.getMainLooper())
    private val whistleSound = Whistle()
    private val sheets by lazy { Sheets(this) { closeSheet() } }
    private var sheet: Sheet? = null
    private var menuOpen = true
    private var safe = Insets.NONE
    private var ime = 0

    private val tick = object : Runnable {
        override fun run() {
            renderTimer()
            if (timer.running) handler.postDelayed(this, 200)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        store = getSharedPreferences("papan", Context.MODE_PRIVATE)
        match = MatchCodec.decode(store.getString("match", null))
        timer = GameTimer(store.getLong("timerMs", 0), store.getLong("timerStart", 0))
        prefs = Prefs(store.getInt("orientation", 0), store.getBoolean("vibrate", true))
        menuOpen = store.getBoolean("menu", true)

        root = FrameLayout(this)
        board = BoardView(this, this)
        overlay = FrameLayout(this).apply {
            setBackgroundColor(Ui.alpha(Ui.INK, 0.6f))
            visibility = View.GONE
            isClickable = true
            setOnClickListener { closeSheet() }
        }
        snackbar = FrameLayout(this)
        root.addView(board, FrameLayout.LayoutParams(MATCH, MATCH))
        root.addView(snackbar, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        root.addView(overlay, FrameLayout.LayoutParams(MATCH, MATCH))
        root.setOnApplyWindowInsetsListener { _, insets -> applyInsets(insets); insets }
        // Panel yang terbuka disusun ulang begitu ukuran layar benar-benar berganti (HP diputar).
        root.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
            if (sheet != null && (r - l != or - ol || b - t != ob - ot)) root.post { buildSheet() }
        }
        setContentView(root)

        @Suppress("DEPRECATION") // masih perlu untuk Android 11-14
        window.setDecorFitsSystemWindows(false)
        applyOrientation()
        board.setMenuOpen(menuOpen)
        render()
    }

    override fun onResume() {
        super.onResume()
        hideBars()
        handler.removeCallbacks(tick)
        tick.run()
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        whistleSound.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideBars()
    }

    @Deprecated("Cukup untuk satu Activity dengan panel sendiri")
    override fun onBackPressed() {
        if (sheet != null) closeSheet()
        else @Suppress("DEPRECATION") super.onBackPressed()
    }

    // ------------------------------------------------------------- tampilan

    private fun render() {
        board.bind(match)
        renderTimer()
    }

    private fun renderTimer() =
        board.bindTimer(GameTimer.format(timer.elapsed(System.currentTimeMillis())), timer.running)

    private fun changed() {
        store.edit().putString("match", MatchCodec.encode(match)).apply()
        render()
    }

    private fun saveTimer() {
        store.edit().putLong("timerMs", timer.accumulated).putLong("timerStart", timer.startedAt).apply()
        handler.removeCallbacks(tick)
        tick.run()
    }

    private fun applyOrientation() {
        requestedOrientation = when (prefs.orientation) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            2 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        }
    }

    private fun hideBars() {
        window.insetsController?.apply {
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsets.Type.systemBars())
        }
    }

    private fun applyInsets(insets: WindowInsets) {
        safe = insets.getInsets(WindowInsets.Type.displayCutout())
        ime = insets.getInsets(WindowInsets.Type.ime()).bottom
        board.setSafeArea(safe)
        overlay.setPadding(safe.left, safe.top, safe.right, max(safe.bottom, ime))
        fitKeyboard()
    }

    /** Landscape + keyboard: sisakan ruang untuk kolom yang sedang diketik. */
    private fun fitKeyboard() {
        val card = overlay.getChildAt(0) as? ViewGroup ?: return
        val tight = ime > 0 && root.width > root.height
        for (i in 0 until card.childCount) {
            val v = card.getChildAt(i)
            if (v.tag == HIDE_ON_IME) v.visibility = if (tight) View.GONE else View.VISIBLE
        }
    }

    // ------------------------------------------------------------- panel

    private fun show(s: Sheet) {
        hideSnackbar()
        sheet = s
        buildSheet()
        overlay.visibility = View.VISIBLE
        board.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(140).start()
    }

    private fun buildSheet() {
        val s = sheet ?: return
        val landscape = root.width > root.height
        val m = dp(12)
        val card = s.build(landscape).apply {
            background = Ui.shape(Ui.WHITE, dp(28).toFloat())
            clipToOutline = true
            isClickable = true
        }
        overlay.removeAllViews()
        val avail = root.width - safe.left - safe.right - 2 * m
        overlay.addView(
            card,
            FrameLayout.LayoutParams(min(dp(s.widthDp(landscape)), avail), WRAP, Gravity.CENTER)
                .apply { setMargins(m, m, m, m) }
        )
        fitKeyboard()
    }

    private fun closeSheet() {
        if (sheet == null) return
        sheet = null
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(root.windowToken, 0)
        overlay.animate().cancel()
        overlay.visibility = View.GONE
        board.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        overlay.removeAllViews()
        hideBars()
    }

    private val hideSnack = Runnable { hideSnackbar() }

    private fun hideSnackbar() {
        handler.removeCallbacks(hideSnack)
        snackbar.removeAllViews()
    }

    private fun snack(message: String) {
        hideSnackbar()
        val r = dp(18).toFloat()
        val bar = Ui.row(this).apply {
            background = Ui.shape(Ui.INK, r)
            setPadding(dp(18), dp(4), dp(6), dp(4))
            addView(
                Ui.oneLine(Ui.text(context, message, 15f, Ui.WHITE)).apply { maxWidth = dp(220) },
                context.lp(WRAP, WRAP)
            )
            addView(Ui.button(context, "Urungkan", Ui.INK, 0xFFFFD34D.toInt()) { hideSnackbar(); undo() }, context.lp(WRAP, dp(48)))
        }
        snackbar.addView(bar, FrameLayout.LayoutParams(WRAP, WRAP).apply {
            setMargins(dp(12), dp(12), dp(12), dp(16) + safe.bottom)
        })
        handler.postDelayed(hideSnack, 4500)
    }

    // ------------------------------------------------------------- aksi

    override fun openCalc(team: Int) {
        val t = match.state.teams[team]
        show(sheets.calculator(t) { value ->
            val reached = match.addPoints(team, value)
            changed()
            if (reached) buzz()
        })
    }

    override fun editTeam(team: Int) = show(
        sheets.editTeam(team, match.state.teams[team]) { name, color, emoji ->
            match.editTeam(team, name, color, emoji)
            changed()
        }
    )

    override fun winSet(team: Int) {
        val name = match.state.teams[team].name
        match.winSet(team)
        changed()
        snack("Set untuk $name")
    }

    override fun removeSet(team: Int) {
        if (match.state.teams[team].sets == 0) return
        val name = match.state.teams[team].name
        match.removeSet(team)
        changed()
        snack("Set $name dikurangi")
    }

    override fun toggleTimer() {
        timer.toggle(System.currentTimeMillis())
        saveTimer()
    }

    override fun resetTimer() {
        timer.reset()
        saveTimer()
    }

    override fun toggleMenu() {
        menuOpen = !menuOpen
        store.edit().putBoolean("menu", menuOpen).apply()
        board.setMenuOpen(menuOpen)
    }

    override fun newMatch() = show(
        sheets.confirm(
            "Mulai match baru?",
            "Poin, set, riwayat, dan timer kembali ke 0. Nama dan warna kubu tetap.",
            "Match baru",
        ) {
            match.newMatch()
            timer.reset()
            saveTimer()
            changed()
            hideSnackbar()
        }
    )

    override fun undo() {
        hideSnackbar()
        if (match.undo()) changed()
    }

    override fun swap() {
        match.swap()
        changed()
    }

    override fun history() = show(sheets.history(match.state))

    override fun whistle() = whistleSound.play()

    override fun settings() = show(
        sheets.settings(match.state.maxPoint, prefs) { max, p ->
            prefs = p
            store.edit().putInt("orientation", p.orientation).putBoolean("vibrate", p.vibrate).apply()
            match.setMaxPoint(max)
            changed()
            applyOrientation()
        }
    )

    private fun buzz() {
        if (!prefs.vibrate) return
        @Suppress("DEPRECATION")
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (v.hasVibrator()) v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 160, 90, 320), -1))
    }
}
