package com.pointrecord.app

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Simpan/muat pertandingan. Data yang rusak dianggap kosong, bukan bikin crash. */
object MatchCodec {
    fun encode(match: Match): String = JSONObject()
        .put("state", state(match.state))
        .put("undo", JSONArray().also { arr -> match.undoList.forEach { arr.put(state(it)) } })
        .toString()

    fun decode(text: String?): Match {
        if (text.isNullOrBlank()) return Match()
        return try {
            val root = JSONObject(text)
            val undo = root.optJSONArray("undo") ?: JSONArray()
            Match(state(root.getJSONObject("state")), undo.objects().map { state(it) })
        } catch (e: JSONException) {
            Match()
        }
    }

    private fun state(s: MatchState) = JSONObject()
        .put("max", s.maxPoint)
        .put("teams", JSONArray().also { arr ->
            s.teams.forEach {
                arr.put(
                    JSONObject().put("name", it.name).put("color", it.color).put("emoji", it.emoji)
                        .put("score", it.score).put("sets", it.sets)
                )
            }
        })
        .put("history", JSONArray().also { arr ->
            s.history.forEach { arr.put(JSONObject().put("w", it.winner).put("a", it.a).put("b", it.b)) }
        })

    private fun state(o: JSONObject): MatchState {
        val teams = o.getJSONArray("teams").objects().map {
            Team(
                it.getString("name"), it.getInt("color"), it.getString("emoji"),
                it.getInt("score"), it.getInt("sets"),
            )
        }
        if (teams.size != 2) throw JSONException("jumlah kubu salah")
        val history = o.getJSONArray("history").objects()
            .map { SetResult(it.getInt("w").coerceIn(0, 1), it.getInt("a"), it.getInt("b")) }
        return MatchState(teams, o.getInt("max"), history)
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
