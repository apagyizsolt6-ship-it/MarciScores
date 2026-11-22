package com.marciscores.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LiveBg = Color(0xFF070B18)
private val LivePanel = Color(0xFF111A2E)
private val LivePanelAlt = Color(0xFF18243E)
private val LiveGreen = Color(0xFF20E875)
private val LiveMuted = Color(0xFF9EABC7)
private val LiveAmber = Color(0xFFFFB84D)

private data class DayOption(
    val offset: Int,
    val label: String
)

private val DayOptions = listOf(
    DayOption(-1, "Tegnap (-1)"),
    DayOption(0, "Ma (0)"),
    DayOption(1, "Holnap (+1)"),
    DayOption(2, "+2 nap"),
    DayOption(3, "+3 nap"),
    DayOption(4, "+4 nap"),
    DayOption(5, "+5 nap"),
    DayOption(6, "+6 nap"),
    DayOption(7, "+7 nap")
)

@Composable
fun LiveMatchesScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val api = remember { StatPalApi() }
    val scope = rememberCoroutineScope()

    var selectedOffset by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var onlyLive by remember { mutableStateOf(false) }
    var onlyFavorites by remember { mutableStateOf(false) }
    var onlyTopLeagues by remember { mutableStateOf(false) }

    var liveMatches by remember { mutableStateOf<List<LiveMatch>>(emptyList()) }
    var dailyMatches by remember { mutableStateOf<List<LiveMatch>>(emptyList()) }
    var debugSummary by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf("") }

    suspend fun refreshMatches(targetOffset: Int = selectedOffset) {
        if (loading) return
        loading = true
        error = null
        try {
            val key = ApiKeyStorage.load(context)
            val liveResult = api.getLiveMatchesWithDiagnostics(key)
            liveMatches = liveResult.matches

            val notes = mutableListOf(liveResult.diagnosticInfo)

            val dailyResult = try {
                api.getDailyMatchesWithDiagnostics(key, offset = targetOffset)
            } catch (e: Exception) {
                notes.add("daily(offset=$targetOffset) hiba: ${e.message}")
                null
            }

            if (dailyResult != null) {
                notes.add(dailyResult.diagnosticInfo)
                dailyMatches = dailyResult.matches
            } else {
                dailyMatches = emptyList()
            }

            debugSummary = notes.joinToString("\n")
            lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        } catch (exception: Exception) {
            error = exception.message ?: "Nem sikerült lekérni a mérkőzéseket."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selectedOffset) {
        refreshMatches(selectedOffset)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            refreshMatches(selectedOffset)
        }
    }

    val isUsingLive = onlyLive || (liveMatches.isNotEmpty() && selectedOffset == 0)
    val baseList = when {
        onlyLive -> liveMatches
        selectedOffset == 0 && liveMatches.isNotEmpty() -> liveMatches
        else -> dailyMatches
    }

    val favs = remember(onlyFavorites) { FavoritesStorage.getFavorites(context) }

    val filteredMatches = remember(baseList, searchQuery, onlyFavorites, onlyTopLeagues, favs) {
        val q = searchQuery.trim().lowercase()
        val topKeywords = listOf("premier", "la liga", "serie a", "bundesliga", "ligue 1", "champions", "europa", "conference", "mls", "eredivisie")

        baseList.filter { m ->
            val matchesSearch = q.isBlank() ||
                m.homeTeam.lowercase().contains(q) ||
                m.awayTeam.lowercase().contains(q) ||
                m.league.lowercase().contains(q) ||
                m.country.lowercase().contains(q)

            val matchesFav = !onlyFavorites ||
                favs.contains(m.homeTeam) || favs.contains(m.awayTeam)

            val matchesTop = !onlyTopLeagues ||
                topKeywords.any { kw -> m.league.lowercase().contains(kw) || m.country.lowercase().contains(kw) }

            matchesSearch && matchesFav && matchesTop
        }
    }

    val grouped = remember(filteredMatches) {
        filteredMatches
            .groupBy { "${it.country.ifBlank { "Egyéb" }} · ${it.league.ifBlank { "Ismeretlen bajnokság" }}" }
            .toList()
            .sortedBy { it.first }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LiveBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ÉLŐ ÉS NAPI MÉRKŐZÉSEK",
                    color = LiveGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Eredmények és program",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )
                if (lastUpdated.isNotBlank()) {
                    Text(
                        text = "Frissítve: $lastUpdated · auto 30 mp",
                        color = LiveMuted,
                        fontSize = 10.sp
                    )
                }
            }
            TextButton(onClick = { onOpenSettings() }) {
                Text("⚙ API", color = LiveGreen)
            }
        }

        // Napváltó sáv (-1..+7)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = onlyLive,
                    onClick = { onlyLive = !onlyLive },
                    label = {
                        Text(
                            text = "● Élő (${liveMatches.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = LivePanel,
                        labelColor = LiveMuted,
                        selectedContainerColor = LiveGreen,
                        selectedLabelColor = Color(0xFF07130D)
                    )
                )
            }
            item {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = { onlyFavorites = !onlyFavorites },
                    label = {
                        Text(
                            text = "⭐ Kedvencek",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = LivePanel,
                        labelColor = LiveMuted,
                        selectedContainerColor = Color(0xFFFFD700),
                        selectedLabelColor = Color(0xFF07130D)
                    )
                )
            }
            item {
                FilterChip(
                    selected = onlyTopLeagues,
                    onClick = { onlyTopLeagues = !onlyTopLeagues },
                    label = {
                        Text(
                            text = "🏆 TOP ligák",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = LivePanel,
                        labelColor = LiveMuted,
                        selectedContainerColor = Color(0xFF4DA6FF),
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(DayOptions, key = { it.offset }) { option ->
                val isSelected = !onlyLive && selectedOffset == option.offset
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onlyLive = false
                        selectedOffset = option.offset
                    },
                    label = {
                        Text(
                            text = option.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = LivePanel,
                        labelColor = LiveMuted,
                        selectedContainerColor = LivePanelAlt,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Gyors kereső
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            singleLine = true,
            placeholder = {
                Text("Keresés csapatra, bajnokságra, országra…", color = LiveMuted, fontSize = 13.sp)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LivePanel,
                unfocusedContainerColor = LivePanel,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = LiveGreen,
                unfocusedBorderColor = LivePanelAlt,
                cursorColor = LiveGreen
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Állapotsor + Frissítés
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "●",
                color = if (isUsingLive && filteredMatches.isNotEmpty()) LiveGreen else LiveAmber,
                fontSize = 12.sp
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = when {
                    onlyLive -> "Élő szűrő: ${filteredMatches.size} meccs"
                    selectedOffset == 0 && liveMatches.isNotEmpty() -> "Élő meccsek: ${filteredMatches.size}"
                    else -> "Nap (offset=$selectedOffset): ${filteredMatches.size} meccs"
                },
                color = LiveMuted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = { scope.launch { refreshMatches(selectedOffset) } },
                enabled = !loading
            ) {
                Text(if (loading) "Frissítés…" else "↻ Frissítés", color = LiveGreen)
            }
        }

        if (error != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LivePanel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Nem sikerült betölteni", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(error.orEmpty(), color = LiveMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = LiveGreen)
                    ) {
                        Text("API-kulcs beállítása", color = Color(0xFF07130D))
                    }
                }
            }
        } else if (loading && filteredMatches.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 55.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LiveGreen)
            }
        } else if (filteredMatches.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LivePanel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚽", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Nincs találat a keresésre" else "Nincs mérkőzés erre a napra/szűrőre",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Próbálj másik napot választani a felső sávban.",
                        color = LiveMuted,
                        fontSize = 12.sp
                    )
                    if (debugSummary.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = debugSummary,
                            color = LiveMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                val expandedLeagues = remember { mutableStateMapOf<String, Boolean>() }

                grouped.forEach { (leagueKey, matches) ->
                    val isExpanded = expandedLeagues.getOrDefault(leagueKey, true)
                    item(key = "header-$leagueKey") {
                        val first = matches.firstOrNull()
                        val leagueColor = leagueAccentColor(leagueKey)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(leagueColor.copy(alpha = 0.18f))
                                .clickable { expandedLeagues[leagueKey] = !isExpanded }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!first?.leagueLogo.isNullOrBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(first!!.leagueLogo),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(3.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(8.dp))
                            } else if (!first?.countryFlag.isNullOrBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(first!!.countryFlag),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(2.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = first?.league?.ifBlank { "Bajnokság" } ?: "Bajnokság",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!first?.country.isNullOrBlank()) {
                                    Text(
                                        text = first!!.country.uppercase(),
                                        color = LiveMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text("${matches.size}", color = leagueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isExpanded) "▼" else "▶", color = leagueColor, fontSize = 10.sp)
                        }
                    }
                    if (isExpanded) {
                        items(
                            items = matches,
                            key = { "${it.id}_${it.homeTeam}_${it.awayTeam}" }
                        ) { match ->
                            LiveMatchCard(
                                match = match,
                                isLive = isUsingLive,
                                leagueColor = leagueAccentColor(leagueKey)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveMatchCard(
    match: LiveMatch,
    isLive: Boolean,
    leagueColor: Color = LiveGreen
) {
    val context = LocalContext.current
    var isHomeFav by remember(match.homeTeam) { mutableStateOf(FavoritesStorage.isFavorite(context, match.homeTeam)) }
    var isAwayFav by remember(match.awayTeam) { mutableStateOf(FavoritesStorage.isFavorite(context, match.awayTeam)) }
    var expanded by remember { mutableStateOf(false) }

    val isLiveMatch = match.status == "ÉLŐ" || isLive
    val statusColor = when {
        isLiveMatch -> LiveGreen
        match.status.contains("Vége", ignoreCase = true) -> LiveMuted
        else -> LiveAmber
    }
    val statusText = when {
        isLiveMatch && match.startTime.isNotBlank() -> match.startTime
        match.status.isNotBlank() -> match.status
        match.startTime.isNotBlank() -> match.startTime
        else -> "–"
    }

    val homeColor = teamColor(match.homeTeam)
    val awayColor = teamColor(match.awayTeam)
    val hasFavorite = isHomeFav || isAwayFav

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (hasFavorite) Color(0xFF1A2E1A) else LivePanel
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Liga színű csík bal oldalon
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(leagueColor)
                )

                // Státusz / perc
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLiveMatch) {
                        Text("●", color = LiveGreen, fontSize = 10.sp)
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Csapatok
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(homeColor, RoundedCornerShape(1.dp)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = match.homeTeam,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isHomeFav) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isHomeFav) Text("★", color = Color(0xFFFFD700), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(awayColor, RoundedCornerShape(1.dp)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = match.awayTeam,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isAwayFav) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAwayFav) Text("★", color = Color(0xFFFFD700), fontSize = 11.sp)
                    }
                }

                // Eredmény
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(42.dp)
                ) {
                    Text(
                        text = match.homeScore.ifBlank { "-" },
                        color = if (isLiveMatch) LiveGreen else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = match.awayScore.ifBlank { "-" },
                        color = if (isLiveMatch) LiveGreen else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Kedvenc
                TextButton(
                    onClick = {
                        val team = if (isHomeFav || !isAwayFav) match.homeTeam else match.awayTeam
                        FavoritesStorage.toggle(context, team)
                        if (team == match.homeTeam) isHomeFav = !isHomeFav else isAwayFav = !isAwayFav
                    },
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.width(32.dp)
                ) {
                    Text(
                        if (hasFavorite) "★" else "☆",
                        color = if (hasFavorite) Color(0xFFFFD700) else LiveMuted,
                        fontSize = 16.sp
                    )
                }
            }

            // Kinyitható részletek
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1528))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (match.venue.isNotBlank()) {
                        Text("📍 ${match.venue}", color = LiveMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (match.matchDate.isNotBlank()) {
                        Text("📅 ${match.matchDate}", color = LiveMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Hazai: ${match.homeScore}", color = homeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Vendég: ${match.awayScore}", color = awayColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (isLiveMatch) "● Élő mérkőzés" else "Státusz: $statusText",
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Koppints a bezáráshoz", color = LiveMuted.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
        }
    }
}
private fun teamColor(name: String): Color {
    val hash = name.hashCode()
    val hue = (hash and 0xFFFFFF) % 360
    return Color.hsv(hue.toFloat(), 0.7f, 0.9f)
}

private fun leagueAccentColor(key: String): Color {
    val k = key.lowercase()
    return when {
        k.contains("premier") -> Color(0xFF3D195B)
        k.contains("la liga") || k.contains("laliga") -> Color(0xFFFF4B44)
        k.contains("serie a") -> Color(0xFF024494)
        k.contains("bundesliga") -> Color(0xFFD20515)
        k.contains("ligue") -> Color(0xFF091C3E)
        k.contains("champions") -> Color(0xFF0E1E5B)
        k.contains("europa") -> Color(0xFFFFA500)
        k.contains("nb i") || k.contains("otp") -> Color(0xFFC8102E)
        else -> Color(0xFF20E875)
    }
}
