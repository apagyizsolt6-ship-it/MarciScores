package com.marciscores.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A normalized live or daily soccer match for the MarciScores UI. */
data class LiveMatch(
    val id: String,
    val country: String,
    val league: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String,
    val awayScore: String,
    val status: String,
    val startTime: String = "",
    val venue: String = ""
)

/** A normalized league standing row from Soccer V2 standings. */
data class LeagueStandingRow(
    val position: String,
    val teamName: String,
    val played: String,
    val won: String,
    val drawn: String,
    val lost: String,
    val goalDifference: String,
    val points: String
)

/** A normalized lineup player entry from Soccer V2 team lineups. */
data class LineupPlayer(
    val name: String,
    val position: String,
    val jerseyNumber: String,
    val isStarter: Boolean
)

/** A normalized team lineup block from Soccer V2 team lineups. */
data class TeamLineup(
    val teamName: String,
    val formation: String,
    val players: List<LineupPlayer>
)

/** A normalized storyline item from Soccer V2 live storylines. */
data class LiveStoryline(
    val id: String,
    val title: String,
    val summary: String,
    val minuteOrTime: String,
    val category: String
)

/** A normalized image reference from Soccer V2 images endpoint. */
data class SoccerImageAsset(
    val url: String,
    val label: String
)

class StatPalApi {

    companion object {
        private const val SOCCER_V2_BASE = "https://statpal.io/api/v2/soccer"
    }

    suspend fun getLiveMatches(apiKey: String): List<LiveMatch> {
        return fetchMatches(
            apiKey = apiKey,
            path = "matches/live",
            queryParams = emptyMap(),
            sortResults = true
        )
    }

    suspend fun getDailyMatches(
        apiKey: String,
        date: String? = null,
        leagueId: String? = null
    ): List<LiveMatch> {
        val params = buildMap {
            date?.trim()?.takeIf { it.isNotBlank() }?.let { put("date", it) }
            leagueId?.trim()?.takeIf { it.isNotBlank() }?.let { put("league_id", it) }
        }
        return fetchMatches(
            apiKey = apiKey,
            path = "matches/daily",
            queryParams = params,
            sortResults = false
        )
    }

    suspend fun getLeagueStandings(
        apiKey: String,
        leagueId: String,
        season: String? = null
    ): List<LeagueStandingRow> = withContext(Dispatchers.IO) {
        val cleanLeagueId = leagueId.trim()
        require(cleanLeagueId.isNotBlank()) { "Add meg a bajnokság azonosítóját (league_id)." }

        val params = buildMap {
            season?.trim()?.takeIf { it.isNotBlank() }?.let { put("season", it) }
        }
        val encodedId = URLEncoder.encode(cleanLeagueId, "UTF-8")
        val responseText = requestGet(apiKey = apiKey, path = "leagues/$encodedId/standings", queryParams = params)
        if (responseText.isBlank()) return@withContext emptyList()

        val rowsArray = extractArray(
            responseText = responseText,
            extraKeys = listOf("standings", "table", "rows", "entries", "positions")
        )
        buildList {
            for (i in 0 until rowsArray.length()) {
                val item = rowsArray.optJSONObject(i) ?: continue
                val teamObj = item.firstObject("team", "club", "participant", "competitor")
                val teamName = item.firstText("team_name", "teamName", "club_name", "name")
                    .ifBlank { teamObj?.firstText("name", "team_name", "short_name").orEmpty() }
                if (teamName.isBlank()) continue

                val pos = item.firstText("position", "rank", "pos", "place").ifBlank { "${i + 1}" }
                val played = item.firstText("played", "matches_played", "mp", "gp", "games").ifBlank { "–" }
                val won = item.firstText("won", "wins", "w").ifBlank { "–" }
                val drawn = item.firstText("drawn", "draws", "d", "ties").ifBlank { "–" }
                val lost = item.firstText("lost", "losses", "l").ifBlank { "–" }
                val gd = item.firstText("goal_difference", "goalDifference", "gd", "diff").ifBlank { "–" }
                val points = item.firstText("points", "pts", "total_points").ifBlank { "–" }

                add(
                    LeagueStandingRow(
                        position = pos,
                        teamName = teamName,
                        played = played,
                        won = won,
                        drawn = drawn,
                        lost = lost,
                        goalDifference = gd,
                        points = points
                    )
                )
            }
        }
    }

    suspend fun getTeamLineups(
        apiKey: String,
        matchId: String? = null,
        teamId: String? = null
    ): List<TeamLineup> = withContext(Dispatchers.IO) {
        val params = buildMap {
            matchId?.trim()?.takeIf { it.isNotBlank() }?.let { put("match_id", it) }
            teamId?.trim()?.takeIf { it.isNotBlank() }?.let { put("team_id", it) }
        }
        val responseText = requestGet(apiKey = apiKey, path = "team/lineups", queryParams = params)
        if (responseText.isBlank()) return@withContext emptyList()

        val rootOrPayload = unwrapPayload(responseText)
        val lineupObjects = mutableListOf<JSONObject>()

        when (rootOrPayload) {
            is JSONArray -> {
                for (i in 0 until rootOrPayload.length()) {
                    rootOrPayload.optJSONObject(i)?.let { lineupObjects.add(it) }
                }
            }
            is JSONObject -> {
                val list = findMatchesArray(rootOrPayload)
                if (list != null) {
                    for (i in 0 until list.length()) {
                        list.optJSONObject(i)?.let { lineupObjects.add(it) }
                    }
                } else {
                    listOf("home", "home_team", "away", "away_team", "lineup", "team").forEach { key ->
                        rootOrPayload.optJSONObject(key)?.let { lineupObjects.add(it) }
                    }
                    if (lineupObjects.isEmpty()) lineupObjects.add(rootOrPayload)
                }
            }
        }

        buildList {
            lineupObjects.forEachIndexed { idx, obj ->
                val teamObj = obj.firstObject("team", "club", "home_team", "away_team")
                val teamName = obj.firstText("team_name", "teamName", "club_name", "name")
                    .ifBlank { teamObj?.firstText("name", "team_name", "short_name").orEmpty() }
                    .ifBlank { "Csapat ${idx + 1}" }
                val formation = obj.firstText("formation", "system", "tactics").ifBlank { "–" }

                val players = mutableListOf<LineupPlayer>()
                val startersArray = obj.optJSONArray("starting_xi")
                    ?: obj.optJSONArray("starters")
                    ?: obj.optJSONArray("startXI")
                    ?: obj.optJSONArray("lineup")
                val benchArray = obj.optJSONArray("substitutes")
                    ?: obj.optJSONArray("bench")
                    ?: obj.optJSONArray("subs")

                fun appendPlayers(arr: JSONArray?, isStarterDefault: Boolean) {
                    if (arr == null) return
                    for (pIdx in 0 until arr.length()) {
                        val pObj = arr.optJSONObject(pIdx)
                        val playerSub = pObj?.firstObject("player")
                        val name = pObj?.firstText("player_name", "playerName", "name")
                            ?.ifBlank { playerSub?.firstText("name", "player_name").orEmpty() }
                            ?: arr.optString(pIdx).orEmpty()
                        if (name.isBlank() || name == "null") continue

                        val pos = pObj?.firstText("position", "pos", "role").orEmpty()
                        val num = pObj?.firstText("jersey_number", "jerseyNumber", "number", "shirt_number").orEmpty()
                        val starter = pObj?.optBoolean("starter", isStarterDefault) ?: isStarterDefault
                        players.add(
                            LineupPlayer(
                                name = name,
                                position = pos,
                                jerseyNumber = num,
                                isStarter = starter
                            )
                        )
                    }
                }

                appendPlayers(startersArray, true)
                appendPlayers(benchArray, false)

                if (players.isEmpty()) {
                    val genericPlayers = obj.optJSONArray("players")
                    appendPlayers(genericPlayers, true)
                }

                if (players.isNotEmpty() || obj.has("formation") || obj.has("team_name") || teamObj != null) {
                    add(TeamLineup(teamName = teamName, formation = formation, players = players))
                }
            }
        }
    }

    suspend fun getLiveStorylines(
        apiKey: String,
        matchId: String? = null
    ): List<LiveStoryline> = withContext(Dispatchers.IO) {
        val params = buildMap {
            matchId?.trim()?.takeIf { it.isNotBlank() }?.let { put("match_id", it) }
        }
        val responseText = requestGet(apiKey = apiKey, path = "live/storylines", queryParams = params)
        if (responseText.isBlank()) return@withContext emptyList()

        val itemsArray = extractArray(
            responseText = responseText,
            extraKeys = listOf("storylines", "stories", "highlights", "events", "updates", "feed")
        )
        buildList {
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.optJSONObject(i) ?: continue
                val title = item.firstText("title", "headline", "heading", "summary", "text", "description")
                if (title.isBlank()) continue
                val summary = item.firstText("summary", "description", "text", "body", "detail")
                    .takeIf { it != title }
                    .orEmpty()
                val time = item.firstText("minute", "elapsed", "time", "timestamp", "clock")
                val category = item.firstText("type", "category", "tag", "event_type")
                val id = item.firstText("id", "storyline_id", "event_id").ifBlank { "story-$i" }
                add(
                    LiveStoryline(
                        id = id,
                        title = title,
                        summary = summary,
                        minuteOrTime = time,
                        category = category
                    )
                )
            }
        }
    }

    suspend fun getImages(
        apiKey: String,
        queryParams: Map<String, String> = emptyMap()
    ): List<SoccerImageAsset> = withContext(Dispatchers.IO) {
        val responseText = requestGet(apiKey = apiKey, path = "images", queryParams = queryParams)
        if (responseText.isBlank()) return@withContext emptyList()

        val arr = extractArray(
            responseText = responseText,
            extraKeys = listOf("images", "assets", "urls", "logos", "photos")
        )
        buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                if (obj != null) {
                    val imgUrl = obj.firstText("url", "image_url", "imageUrl", "src", "link", "image")
                    if (imgUrl.isNotBlank()) {
                        val label = obj.firstText("name", "title", "label", "alt", "type").ifBlank { "Kép ${i + 1}" }
                        add(SoccerImageAsset(url = imgUrl, label = label))
                    }
                } else {
                    val rawUrl = arr.optString(i).trim()
                    if (rawUrl.isNotBlank() && rawUrl != "null") {
                        add(SoccerImageAsset(url = rawUrl, label = "Kép ${i + 1}"))
                    }
                }
            }
        }
    }

    private suspend fun fetchMatches(
        apiKey: String,
        path: String,
        queryParams: Map<String, String>,
        sortResults: Boolean
    ): List<LiveMatch> = withContext(Dispatchers.IO) {
        val responseText = requestGet(apiKey = apiKey, path = path, queryParams = queryParams)
        if (responseText.isBlank()) return@withContext emptyList()

        if (responseText.startsWith("[")) {
            val matches = parseMatchesArray(JSONArray(responseText))
            return@withContext if (sortResults) matches.sortedWith(matchComparator()) else matches
        }

        val root = parseJsonObject(responseText)
        ensureSuccess(root)

        val matchesArray = findMatchesArray(root)
        if (matchesArray != null) {
            val matches = parseMatchesArray(matchesArray)
            return@withContext if (sortResults) matches.sortedWith(matchComparator()) else matches
        }

        val candidateObject = root.optJSONObject("data")
            ?: root.optJSONObject("match")
            ?: root.optJSONObject("live_match")
            ?: root.optJSONObject("liveMatch")

        if (candidateObject != null) {
            val singleMatch = candidateObject.toLiveMatch(0)
            if (singleMatch.homeTeam.isNotBlank() && singleMatch.awayTeam.isNotBlank()) {
                return@withContext listOf(singleMatch)
            }
        }

        throw buildUnexpectedStructureException(root)
    }

    private fun requestGet(
        apiKey: String,
        path: String,
        queryParams: Map<String, String>
    ): String {
        require(apiKey.isNotBlank()) { "Először add meg a StatPal API-kulcsot a beállításokban." }

        val encodedKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val extraQuery = queryParams
            .filterValues { it.isNotBlank() }
            .entries
            .joinToString(separator = "") { (k, v) ->
                "&${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v.trim(), "UTF-8")}"
            }

        val cleanPath = path.trimStart('/')
        val url = URL("$SOCCER_V2_BASE/$cleanPath?access_key=$encodedKey$extraQuery")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty().trim()

            if (code !in 200..299) {
                val apiErrMsg = extractApiErrorMessage(responseText)
                when (code) {
                    401, 403 -> throw IllegalStateException(
                        apiErrMsg ?: "A StatPal elutasította az API-kulcsot. Ellenőrizd a beállításokban."
                    )
                    429 -> throw IllegalStateException(
                        apiErrMsg ?: "A StatPal kérési limitje elérte a határt. Próbáld később."
                    )
                    else -> throw IllegalStateException(
                        apiErrMsg ?: "StatPal API-hiba (HTTP $code). Próbáld újra később."
                    )
                }
            }

            return responseText
        } finally {
            connection.disconnect()
        }
    }

    private fun parseJsonObject(responseText: String): JSONObject {
        return try {
            JSONObject(responseText)
        } catch (_: JSONException) {
            throw IllegalStateException(
                "A StatPal válasza nem érvényes JSON formátumú: ${responseText.take(140)}"
            )
        }
    }

    private fun ensureSuccess(root: JSONObject) {
        if (root.optBoolean("success", true).not()) {
            val apiMessage = root.optString("message")
                .ifBlank { root.optString("error") }
                .takeIf { it.isNotBlank() && it != "null" }
            throw IllegalStateException(apiMessage ?: "A StatPal nem tudta lekérni a kért adatokat.")
        }
    }

    private fun unwrapPayload(responseText: String): Any {
        val trimmed = responseText.trim()
        if (trimmed.startsWith("[")) return JSONArray(trimmed)
        val root = parseJsonObject(trimmed)
        ensureSuccess(root)
        return root.opt("data") ?: root
    }

    private fun extractArray(responseText: String, extraKeys: List<String>): JSONArray {
        val trimmed = responseText.trim()
        if (trimmed.startsWith("[")) return JSONArray(trimmed)
        val root = parseJsonObject(trimmed)
        ensureSuccess(root)

        for (key in extraKeys) {
            root.optJSONArray(key)?.let { return it }
            root.optJSONObject("data")?.optJSONArray(key)?.let { return it }
        }

        findMatchesArray(root)?.let { return it }
        throw buildUnexpectedStructureException(root)
    }

    private fun buildUnexpectedStructureException(root: JSONObject): IllegalStateException {
        val rootKeys = buildList {
            val it = root.keys()
            while (it.hasNext()) add(it.next())
        }.joinToString(", ")

        val dataSummary = root.opt("data")?.let { d ->
            when (d) {
                is JSONObject -> {
                    val dKeys = buildList {
                        val it = d.keys()
                        while (it.hasNext()) add(it.next())
                    }.joinToString(", ")
                    "data kulcsai: [$dKeys]"
                }
                else -> "data típusa: ${d.javaClass.simpleName} (${d.toString().take(80)})"
            }
        } ?: "nincs 'data' mező"

        return IllegalStateException(
            "A StatPal válasza nem a várt listát tartalmazza. (Kulcsok: [$rootKeys], $dataSummary)"
        )
    }

    private fun extractApiErrorMessage(responseText: String): String? {
        if (responseText.isBlank()) return null
        return try {
            val obj = JSONObject(responseText)
            obj.optString("message")
                .ifBlank { obj.optString("error") }
                .takeIf { it.isNotBlank() && it != "null" }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMatchesArray(matchesArray: JSONArray): List<LiveMatch> {
        return buildList {
            for (index in 0 until matchesArray.length()) {
                val item = matchesArray.optJSONObject(index) ?: continue
                val match = item.toLiveMatch(index)
                if (match.homeTeam.isNotBlank() && match.awayTeam.isNotBlank()) {
                    add(match)
                }
            }
        }
    }

    private fun matchComparator(): Comparator<LiveMatch> {
        return compareBy<LiveMatch>(
            { it.country.lowercase() },
            { it.league.lowercase() },
            { it.homeTeam.lowercase() }
        )
    }

    private fun findMatchesArray(root: JSONObject): JSONArray? {
        val directKeys = listOf(
            "data",
            "matches",
            "live_matches",
            "liveMatches",
            "results",
            "fixtures",
            "games",
            "events",
            "items",
            "standings",
            "storylines",
            "images"
        )
        for (key in directKeys) {
            root.optJSONArray(key)?.let { return it }
        }
        for (key in directKeys) {
            val nested = root.optJSONObject(key) ?: continue
            for (nestedKey in listOf("data", "matches", "live_matches", "liveMatches", "items", "results", "fixtures", "games", "events", "standings", "storylines", "images")) {
                nested.optJSONArray(nestedKey)?.let { return it }
            }
            val nestedKeys = nested.keys()
            while (nestedKeys.hasNext()) {
                val value = nested.opt(nestedKeys.next())
                if (value is JSONArray) return value
            }
        }
        val keys = root.keys()
        while (keys.hasNext()) {
            val value = root.opt(keys.next())
            if (value is JSONArray) return value
        }
        return null
    }

    private fun JSONObject.toLiveMatch(index: Int): LiveMatch {
        val homeObject = firstObject("home_team", "homeTeam", "home", "team_home", "local_team", "localTeam")
        val awayObject = firstObject("away_team", "awayTeam", "away", "team_away", "visitor_team", "visitorTeam")

        val homeName = firstText("home_team_name", "homeTeamName", "home_name", "homeName")
            .ifBlank { homeObject?.firstText("name", "team_name", "title", "short_name").orEmpty() }
            .ifBlank { firstText("home") }
        val awayName = firstText("away_team_name", "awayTeamName", "away_name", "awayName")
            .ifBlank { awayObject?.firstText("name", "team_name", "title", "short_name").orEmpty() }
            .ifBlank { firstText("away") }

        val scoreObject = firstObject("score", "scores", "current_score", "result", "ft_score")
        val homeScore = firstText("home_score", "homeScore", "home_goals", "homeGoals", "goals_home")
            .ifBlank { scoreObject?.firstText("home", "home_score", "homeScore", "local").orEmpty() }
            .ifBlank { homeObject?.firstText("score", "goals").orEmpty() }
        val awayScore = firstText("away_score", "awayScore", "away_goals", "awayGoals", "goals_away")
            .ifBlank { scoreObject?.firstText("away", "away_score", "awayScore", "visitor").orEmpty() }
            .ifBlank { awayObject?.firstText("score", "goals").orEmpty() }

        val leagueObject = firstObject("league", "competition", "tournament", "cup")
        val countryObject = firstObject("country", "area", "region")
        val venueObject = firstObject("venue", "stadium")

        val league = firstText("league_name", "leagueName", "competition_name", "tournament_name")
            .ifBlank { leagueObject?.firstText("name", "title").orEmpty() }
            .ifBlank { "Bajnokság" }
        val country = firstText("country_name", "countryName")
            .ifBlank { countryObject?.firstText("name", "title").orEmpty() }

        val rawStatus = firstText("status", "match_status", "state", "minute", "elapsed", "time")
        val status = normalizeStatus(rawStatus, this)
        val id = firstText("match_id", "matchId", "id", "fixture_id", "game_id").ifBlank { "live-$index" }
        val startTime = firstText("start_time", "startTime", "kickoff", "date", "time")
        val venue = firstText("venue_name", "venueName", "stadium")
            .ifBlank { venueObject?.firstText("name", "title").orEmpty() }

        return LiveMatch(
            id = id,
            country = country,
            league = league,
            homeTeam = homeName,
            awayTeam = awayName,
            homeScore = homeScore.ifBlank { "–" },
            awayScore = awayScore.ifBlank { "–" },
            status = status,
            startTime = startTime,
            venue = venue
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
