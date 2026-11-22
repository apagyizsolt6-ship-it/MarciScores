package com.marciscores.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A normalized live soccer match for the MarciScores UI. */
data class LiveMatch(
    val id: String,
    val country: String,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String,
    val awayScore: String,
    val status: String
)

class StatPalApi {

    suspend fun getLiveMatches(apiKey: String): List<LiveMatch> = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Először add meg a StatPal API-kulcsot a beállításokban." }

        val encodedKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val url = URL("https://statpal.io/api/v2/soccer/matches/live?access_key=$encodedKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                when (code) {
                    401, 403 -> throw IllegalStateException("A StatPal elutasította az API-kulcsot. Ellenőrizd a beállításokban.")
                    429 -> throw IllegalStateException("A StatPal kérési limitje elérte a határt. Próbáld később.")
                    else -> throw IllegalStateException("StatPal API-hiba (HTTP $code). Próbáld újra később.")
                }
            }

            val root = JSONObject(responseText)
            if (root.optBoolean("success", true).not()) {
                val apiMessage = root.optString("message").takeIf { it.isNotBlank() }
                throw IllegalStateException(apiMessage ?: "A StatPal nem tudta lekérni az élő mérkőzéseket.")
            }

            val matchesArray = findMatchesArray(root)
                ?: throw IllegalStateException("A StatPal válasza nem a várt mérkőzéslistát tartalmazza.")

            buildList {
                for (index in 0 until matchesArray.length()) {
                    val item = matchesArray.optJSONObject(index) ?: continue
                    val match = item.toLiveMatch(index)
                    if (match.homeTeam.isNotBlank() && match.awayTeam.isNotBlank()) add(match)
                }
            }.sortedWith(compareBy<LiveMatch>({ it.country.lowercase() }, { it.league.lowercase() }, { it.homeTeam.lowercase() }))
        } finally {
            connection.disconnect()
        }
    }

    private fun findMatchesArray(root: JSONObject): JSONArray? {
        val directKeys = listOf("data", "matches", "live_matches", "liveMatches", "results")
        for (key in directKeys) {
            root.optJSONArray(key)?.let { return it }
        }
        for (key in directKeys) {
            val nested = root.optJSONObject(key) ?: continue
            for (nestedKey in listOf("data", "matches", "items", "results")) {
                nested.optJSONArray(nestedKey)?.let { return it }
            }
        }
        // Some API versions return the list directly inside a wrapper object.
        val keys = root.keys()
        while (keys.hasNext()) {
            val value = root.opt(keys.next())
            if (value is JSONArray) return value
        }
        return null
    }

    private fun JSONObject.toLiveMatch(index: Int): LiveMatch {
        val homeObject = firstObject("home_team", "homeTeam", "home", "team_home")
        val awayObject = firstObject("away_team", "awayTeam", "away", "team_away")

        val homeName = firstText("home_team_name", "homeTeamName", "home_name")
            .ifBlank { homeObject?.firstText("name", "team_name", "title", "short_name").orEmpty() }
            .ifBlank { firstText("home") }
        val awayName = firstText("away_team_name", "awayTeamName", "away_name")
            .ifBlank { awayObject?.firstText("name", "team_name", "title", "short_name").orEmpty() }
            .ifBlank { firstText("away") }

        val scoreObject = firstObject("score", "scores", "current_score")
        val homeScore = firstText("home_score", "homeScore", "home_goals", "homeGoals")
            .ifBlank { scoreObject?.firstText("home", "home_score", "homeScore").orEmpty() }
            .ifBlank { homeObject?.firstText("score", "goals").orEmpty() }
        val awayScore = firstText("away_score", "awayScore", "away_goals", "awayGoals")
            .ifBlank { scoreObject?.firstText("away", "away_score", "awayScore").orEmpty() }
            .ifBlank { awayObject?.firstText("score", "goals").orEmpty() }

        val leagueObject = firstObject("league", "competition", "tournament")
        val countryObject = firstObject("country")
        val league = firstText("league_name", "leagueName", "competition_name", "tournament_name")
            .ifBlank { leagueObject?.firstText("name", "title").orEmpty() }
            .ifBlank { "Bajnokság" }
        val country = firstText("country_name", "countryName")
            .ifBlank { countryObject?.firstText("name", "title").orEmpty() }

        val rawStatus = firstText("status", "match_status", "state", "minute", "elapsed")
        val status = normalizeStatus(rawStatus, this)
        val id = firstText("match_id", "matchId", "id", "fixture_id").ifBlank { "live-$index" }

        return LiveMatch(
            id = id,
            country = country,
            league = league,
            homeTeam = homeName,
            awayTeam = awayName,
            homeScore = homeScore.ifBlank { "–" },
            awayScore = awayScore.ifBlank { "–" },
            status = status
        )
    }

    private fun normalizeStatus(raw: String, item: JSONObject): String {
        val value = raw.trim()
        if (value.isBlank()) return "ÉLŐ"
        val upper = value.uppercase()
        if (upper == "HT" || upper.contains("HALF TIME") || upper.contains("HALFTIME")) return "SZÜNET"
        if (upper == "FT" || upper.contains("FINISHED") || upper.contains("FULL TIME")) return "VÉGE"
        if (upper.contains("PEN") || upper.contains("PENALTY")) return "TIZ"
        val minute = value.toIntOrNull()
        if (minute != null) return "$minute'"
        val explicitMinute = item.optInt("minute", -1).takeIf { it >= 0 }
        if (explicitMinute != null) return "$explicitMinute'"
        return when (upper) {
            "LIVE", "INPLAY", "IN_PLAY", "IN PLAY" -> "ÉLŐ"
            "1H", "2H", "FIRST_HALF", "SECOND_HALF" -> "ÉLŐ"
            else -> value.take(18)
        }
    }

    private fun JSONObject.firstObject(vararg keys: String): JSONObject? {
        for (key in keys) optJSONObject(key)?.let { return it }
        return null
    }

    private fun JSONObject.firstText(vararg keys: String): String {
        for (key in keys) {
            val value = opt(key) ?: continue
            when (value) {
                is JSONObject -> value.firstText("name", "team_name", "title", "score")
                    .takeIf { it.isNotBlank() }?.let { return it }
                is JSONArray -> Unit
                JSONObject.NULL -> Unit
                else -> value.toString().trim().takeIf { it.isNotBlank() && it != "null" }?.let { return it }
            }
        }
        return ""
    }
}
