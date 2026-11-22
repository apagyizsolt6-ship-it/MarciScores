package com.marciscores.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    val filteredMatches = remember(baseList, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            baseList
        } else {
            baseList.filter { m ->
                m.homeTeam.lowercase().contains(q) ||
                    m.awayTeam.lowercase().contains(q) ||
                    m.league.lowercase().contains(q) ||
                    m.country.lowercase().contains(q)
            }
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
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grouped.forEach { (leagueKey, matches) ->
                    item(key = "header-$leagueKey") {
                        val first = matches.firstOrNull()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!first?.leagueLogo.isNullOrBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(first!!.leagueLogo),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(8.dp))
                            } else if (!first?.countryFlag.isNullOrBlank()) {
                                Image(
                                    painter = rememberAsyncImagePainter(first!!.countryFlag),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = leagueKey,
                                color = LiveGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    items(
                        items = matches,
                        key = { "${it.id}_${it.homeTeam}_${it.awayTeam}" }
                    ) { match ->
                        LiveMatchCard(match = match, isLive = isUsingLive)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveMatchCard(
    match: LiveMatch,
    isLive: Boolean
) {
    val badgeColor = if (isLive) LiveGreen else LiveAmber

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LivePanel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            val headerMeta = buildList {
                if (match.country.isNotBlank()) add(match.country)
                if (match.league.isNotBlank()) add(match.league)
                if (match.venue.isNotBlank()) add(match.venue)
            }.joinToString(" · ")

            Text(
                text = headerMeta,
                color = LiveMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (match.homeLogo.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(match.homeLogo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = match.homeTeam,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = match.homeScore,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (match.awayLogo.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(match.awayLogo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = match.awayTeam,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = match.awayScore,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val badgeText = when {
                    match.status == "ÉLŐ" && match.startTime.isNotBlank() -> match.startTime
                    match.status.isNotBlank() -> match.status
                    match.startTime.isNotBlank() -> match.startTime
                    else -> "PROGRAM"
                }
                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
