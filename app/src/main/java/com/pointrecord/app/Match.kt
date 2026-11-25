package com.pointrecord.app

data class Team(
    val name: String,
    val color: Int,
    val emoji: String,
    val score: Int = 0,
    val sets: Int = 0,
)

/** Skor akhir satu set. [winner] 0 = kubu kiri/atas, 1 = kubu kanan/bawah. */
data class SetResult(val winner: Int, val a: Int, val b: Int)

data class MatchState(
    val teams: List<Team>,
    /** 0 = tanpa batas. */
    val maxPoint: Int,
    val history: List<SetResult> = emptyList(),
) {
    companion object {
        fun default() = MatchState(
            teams = listOf(
                Team("Kubu 1", 0xFF1E88F0.toInt(), "🐯"),
                Team("Kubu 2", 0xFFEF3E36.toInt(), "🦅"),
            ),
            maxPoint = 100,
        )
    }
}

class Match(state: MatchState = MatchState.default(), undo: List<MatchState> = emptyList()) {
    var state: MatchState = state
        private set
    private val undoStack = ArrayDeque(undo)
    val undoList: List<MatchState> get() = undoStack
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    /** True kalau poin ini membuat kubu [i] baru mencapai max poin. */
    fun addPoints(i: Int, n: Long): Boolean {
        val before = state.teams[i].score
        val after = (before + n).coerceIn(MIN_SCORE, MAX_SCORE).toInt()
        change(state.withTeam(i) { it.copy(score = after) })
        val max = state.maxPoint
        return max > 0 && before < max && after >= max
    }

    fun addPoints(i: Int, n: Int): Boolean = addPoints(i, n.toLong())

    /** Set untuk kubu [i], poin kedua kubu balik ke 0. */
    fun winSet(i: Int) {
        val (a, b) = state.teams
        val result = SetResult(i, a.score, b.score)
        change(
            state.copy(
                teams = state.teams.mapIndexed { t, team ->
                    team.copy(score = 0, sets = if (t == i) (team.sets + 1).coerceAtMost(MAX_SETS) else team.sets)
                },
                history = (state.history + result).takeLast(MAX_HISTORY),
            )
        )
    }

    /** Koreksi: set kubu [i] dikurangi 1, poin berjalan tetap. */
    fun removeSet(i: Int) {
        if (state.teams[i].sets == 0) return
        val last = state.history.indexOfLast { it.winner == i }
        change(
            state.withTeam(i) { it.copy(sets = it.sets - 1) }
                .copy(history = state.history.filterIndexed { n, _ -> n != last })
        )
    }

    fun swap() = change(
        state.copy(
            teams = state.teams.reversed(),
            history = state.history.map { SetResult(1 - it.winner, it.b, it.a) },
        )
    )

    fun newMatch() = change(
        state.copy(teams = state.teams.map { it.copy(score = 0, sets = 0) }, history = emptyList())
    )

    fun editTeam(i: Int, name: String, color: Int, emoji: String) = change(
        state.withTeam(i) {
            it.copy(name = name.trim().ifEmpty { "Kubu ${i + 1}" }, color = color, emoji = emoji)
        }
    )

    fun setMaxPoint(n: Int) = change(state.copy(maxPoint = n.coerceIn(0, MAX_SCORE.toInt())))

    /** Kubu yang sudah mencapai max poin dan unggul, atau null. */
    fun leader(): Int? {
        val max = state.maxPoint
        if (max <= 0) return null
        val (a, b) = state.teams.map { it.score }
        return when {
            a >= max && a > b -> 0
            b >= max && b > a -> 1
            else -> null
        }
    }

    fun undo(): Boolean {
        state = undoStack.removeLastOrNull() ?: return false
        return true
    }

    private fun change(next: MatchState) {
        if (next == state) return
        undoStack.addLast(state)
        while (undoStack.size > MAX_UNDO) undoStack.removeFirst()
        state = next
    }

    private fun MatchState.withTeam(i: Int, f: (Team) -> Team) =
        copy(teams = teams.mapIndexed { t, team -> if (t == i) f(team) else team })

    companion object {
        const val MAX_UNDO = 30
        const val MIN_SCORE = -999L
        const val MAX_SCORE = 9999L
        const val MAX_SETS = 99
        const val MAX_HISTORY = 100
    }
}
