package com.pointrecord.app

import org.junit.Assert.*
import org.junit.Test

class LogicTest {
    private fun calc(keys: String) = Calc().also { c -> keys.forEach { c.press(it) } }

    @Test fun calcBasics() {
        assertEquals(45L, calc("25+10*2").value())
        assertEquals(7L, calc("10-3").value())
        assertEquals(-5L, calc("5-10").value())
        assertNull(calc("").value())
    }

    @Test fun calcDanglingOperatorIgnored() {
        assertEquals(25L, calc("25+").value())
        assertEquals("25", calc("25+=").text)
    }

    @Test fun calcOperatorReplacedAndLeadingBlocked() {
        assertEquals("5*", calc("5+*").text)
        assertEquals("", calc("+").text)
        assertEquals("", calc("*").text)
        assertEquals("", calc("-").text)
    }

    @Test fun calcEqualsThenContinue() {
        assertEquals("20", calc("10*2=").text)
        assertEquals(25L, calc("10*2=+5").value())
        assertEquals("7", calc("10*2=7").text)
        assertEquals("-5", calc("5-10=").text)
        assertEquals(1L, calc("5-10=+6").value())
    }

    @Test fun calcLimits() {
        assertEquals("99999", calc("9999999").text)
        assertEquals(99999L, calc("99999*99999*99999").value())
        assertEquals(-99999L, calc("1-99999*99999").value())
        assertTrue(calc("1+".repeat(60)).text.length <= 24)
        assertEquals("7", calc("007").text)
    }

    @Test fun calcEditKeys() {
        assertEquals("1", calc("12<").text)
        assertEquals("", calc("12C").text)
        assertEquals("", calc("<").text)
        // Hasil negatif yang dihapus sampai tinggal "-" tidak boleh bikin error.
        assertNull(calc("5-10=<").value())
    }

    private fun match(max: Int = 100) = Match(MatchState.default().copy(maxPoint = max))

    @Test fun reachingMaxReportsOnceAndNeverResets() {
        val m = match()
        assertFalse(m.addPoints(0, 99))
        assertTrue(m.addPoints(0, 6))
        assertEquals(105, m.state.teams[0].score)
        assertFalse(m.addPoints(0, 1))
        assertEquals(0, m.state.teams[0].sets)
        assertEquals(0, m.leader())
    }

    @Test fun leaderRules() {
        val m = match()
        assertNull(m.leader())
        m.addPoints(0, 100)
        m.addPoints(1, 100)
        assertNull(m.leader())
        m.addPoints(1, 5)
        assertEquals(1, m.leader())
        val off = match(0)
        assertFalse(off.addPoints(0, 500))
        assertNull(off.leader())
    }

    @Test fun scoreClamped() {
        val m = match()
        m.addPoints(0, 99999)
        assertEquals(9999, m.state.teams[0].score)
        m.addPoints(0, -99999)
        assertEquals(-999, m.state.teams[0].score)
    }

    @Test fun winSetResetsBothAndUndoRestores() {
        val m = match()
        m.addPoints(0, 105)
        m.addPoints(1, 80)
        m.winSet(0)
        assertEquals(listOf(0, 0), m.state.teams.map { it.score })
        assertEquals(listOf(1, 0), m.state.teams.map { it.sets })
        assertEquals(listOf(SetResult(0, 105, 80)), m.state.history)
        assertTrue(m.undo())
        assertEquals(listOf(105, 80), m.state.teams.map { it.score })
        assertEquals(0, m.state.teams[0].sets)
        assertTrue(m.state.history.isEmpty())
    }

    @Test fun removeSetKeepsPointsAndDropsLatestHistoryEntry() {
        val m = match()
        m.winSet(0)
        m.addPoints(1, 100)
        m.winSet(1)
        m.winSet(0)
        m.addPoints(0, 7)
        m.removeSet(1)
        assertEquals(7, m.state.teams[0].score)
        assertEquals(listOf(2, 0), m.state.teams.map { it.sets })
        assertEquals(listOf(0, 0), m.state.history.map { it.winner })
        m.removeSet(1)
        assertEquals(0, m.state.teams[1].sets)
    }

    @Test fun swapFlipsTeamsAndHistory() {
        val m = match()
        m.addPoints(0, 100)
        m.addPoints(1, 40)
        m.winSet(0)
        val name = m.state.teams[0].name
        m.swap()
        assertEquals(name, m.state.teams[1].name)
        assertEquals(1, m.state.teams[1].sets)
        assertEquals(SetResult(1, 40, 100), m.state.history[0])
    }

    @Test fun newMatchKeepsIdentity() {
        val m = match()
        m.editTeam(0, "Garuda", 0x112233, "X")
        m.addPoints(0, 10)
        m.winSet(0)
        m.newMatch()
        assertEquals("Garuda", m.state.teams[0].name)
        assertEquals(0, m.state.teams[0].sets)
        assertTrue(m.state.history.isEmpty())
        assertEquals(100, m.state.maxPoint)
    }

    @Test fun blankNameFallsBack() {
        val m = match()
        m.editTeam(1, "   ", 1, "")
        assertEquals("Kubu 2", m.state.teams[1].name)
    }

    @Test fun undoEmptyAndBounded() {
        val m = match()
        assertFalse(m.undo())
        repeat(200) { m.addPoints(0, 1) }
        var n = 0
        while (m.undo()) n++
        assertEquals(Match.MAX_UNDO, n)
    }

    @Test fun noopChangesDoNotFillUndo() {
        val m = match()
        m.addPoints(0, 0)
        m.removeSet(0)
        m.setMaxPoint(100)
        assertFalse(m.undo())
    }

    @Test fun codecRoundTripWithUndo() {
        val m = match(50)
        m.editTeam(0, "A \"x\" é", -123, "🐯")
        m.addPoints(0, 60)
        m.winSet(0)
        val r = MatchCodec.decode(MatchCodec.encode(m))
        assertEquals(m.state, r.state)
        assertTrue(r.undo())
        assertEquals(60, r.state.teams[0].score)
    }

    @Test fun codecSurvivesGarbage() {
        for (s in listOf(null, "", "rusak", "{}", "{\"state\":{\"teams\":[]}}", "[1]"))
            assertEquals(MatchState.default(), MatchCodec.decode(s).state)
    }

    @Test fun timer() {
        val t = GameTimer()
        t.toggle(1000)
        assertTrue(t.running)
        assertEquals(500L, t.elapsed(1500))
        t.toggle(3000)
        assertFalse(t.running)
        assertEquals(2000L, t.elapsed(9000))
        t.toggle(10000)
        assertEquals(3000L, t.elapsed(11000))
        assertEquals(2000L, t.elapsed(5000)) // jam HP mundur
        t.reset()
        assertEquals(0L, t.elapsed(1))
        assertFalse(t.running)
        assertEquals("00:01", GameTimer.format(1999))
        assertEquals("59:59", GameTimer.format(3_599_000))
        assertEquals("1:00:00", GameTimer.format(3_600_000))
    }
}
