package com.pointrecord.app

/** Kalkulator bilangan bulat. Kunci: 0-9 + - * = C (hapus semua) < (hapus satu). */
class Calc(expr: String = "") {
    var text: String = expr
        private set

    /** True setelah "=": angka berikutnya memulai hitungan baru. */
    private var fresh = false

    fun press(key: Char) {
        when (key) {
            in '0'..'9' -> digit(key)
            '+', '-', '*' -> operator(key)
            '=' -> value()?.let { text = it.toString(); fresh = true }
            'C' -> { text = ""; fresh = false }
            '<' -> { text = text.dropLast(1); fresh = false }
        }
    }

    private fun digit(d: Char) {
        if (fresh) { text = ""; fresh = false }
        val number = text.takeLastWhile { it.isDigit() }
        when {
            number == "0" -> text = text.dropLast(1) + d
            number.length < MAX_DIGITS && text.length < MAX_LENGTH -> text += d
        }
    }

    private fun operator(op: Char) {
        if (text.isEmpty() || text == "-") return
        fresh = false
        when {
            !text.last().isDigit() -> text = text.dropLast(1) + op
            text.length < MAX_LENGTH -> text += op
        }
    }

    /** Hasil hitungan (perkalian didahulukan), atau null kalau belum ada angka. */
    fun value(): Long? {
        val expr = text.dropLastWhile { !it.isDigit() }
        if (expr.isEmpty()) return null
        var sum = 0L
        var product = 0L
        var sign = 1L
        var multiply = false
        var i = 0
        if (expr[0] == '-') { sign = -1; i = 1 }
        while (i < expr.length) {
            val start = i
            while (i < expr.length && expr[i].isDigit()) i++
            val n = expr.substring(start, i).toLongOrNull() ?: return null
            product = if (multiply) (product * n).coerceIn(-INTERNAL, INTERNAL) else sign * n
            if (i == expr.length) break
            when (expr[i++]) {
                '*' -> multiply = true
                '+' -> { sum += product; sign = 1; multiply = false }
                else -> { sum += product; sign = -1; multiply = false }
            }
        }
        return (sum + product).coerceIn(-LIMIT, LIMIT)
    }

    private companion object {
        const val MAX_DIGITS = 5
        const val MAX_LENGTH = 24
        const val LIMIT = 99_999L
        const val INTERNAL = 1_000_000_000_000L
    }
}
