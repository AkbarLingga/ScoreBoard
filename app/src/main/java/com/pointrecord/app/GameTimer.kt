package com.pointrecord.app

/** Stopwatch berbasis jam dinding, supaya tetap jalan walau aplikasi ditutup. */
class GameTimer(var accumulated: Long = 0, var startedAt: Long = 0) {
    val running: Boolean get() = startedAt > 0

    fun toggle(now: Long) {
        if (running) {
            accumulated = elapsed(now)
            startedAt = 0
        } else {
            startedAt = now
        }
    }

    fun reset() {
        accumulated = 0
        startedAt = 0
    }

    fun elapsed(now: Long): Long =
        accumulated + if (running) (now - startedAt).coerceAtLeast(0) else 0

    companion object {
        fun format(ms: Long): String {
            val s = ms / 1000
            val mm = (s / 60 % 60).toString().padStart(2, '0')
            val ss = (s % 60).toString().padStart(2, '0')
            return if (s >= 3600) "${s / 3600}:$mm:$ss" else "$mm:$ss"
        }
    }
}
