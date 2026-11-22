package com.marciscores.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LiveBg = Color(0xFF070B18)
private val LivePanel = Color(0xFF111A2E)
private val LiveGreen = Color(0xFF20E875)
private val LiveMuted = Color(0xFF9EABC7)
private val LiveAmber = Color(0xFFFFB84D)

@Composable
fun LiveMatchesScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val api = remember { StatPalApi() }
    val scope = rememberCoroutineScope()

    var liveMatches by remember { mutableStateOf<List<LiveMatch>>(emptyList()) }
    var dailyFallbackMatches by remember { mutableStateOf<List<LiveMatch>>(emptyList()) }
    var showingDailyFallback by remember { mutableStateOf(false) }
    var debugSummary by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf("") }

    suspend fun refreshMatches() {
        if (loading) return
        loading = true
        error = null
        try {
            val key = ApiKeyStorage.load(context)
            val liveResult = api.getLiveMatchesWithDiagnostics(key)
            liveMatches = liveResult.matches

            val notes = mutableListOf(liveResult.diagnosticInfo)

            if (liveResult.matches.isEmpty()) {
                val dailyResult = try {
                    api.getDailyMatchesWithDiagnostics(key, offset = -1)
                } catch (e: Exception) {
                    notes.add("daily(offset=-1) hiba: ${e.message}")
                    null
                }

                if (dailyResult != null) {
                    notes.add(dailyResult.diagnosticInfo)
                    dailyFallbackMatches = dailyResult.matches
                    showingDailyFallback = dailyResult.matches.isNotEmpty()
                } else {
                    dailyFallbackMatches = emptyList()
                    showingDailyFallback = false
                }
            } else {
                dailyFallbackMatches = emptyList()
                showingDailyFallback = false
            }

            debugSummary = notes.joinToString("\n")
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            lastUpdated = timeFormat.format(Date())
        } catch (exception: Exception) {
            error = exception.message ?: "Nem sikerült lekérni az élő mérkőzéseket."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshMatches()
        while (true) {
            delay(30_000)
            refreshMatches()
        }
    }

    val displayedMatches = if (liveMatches.isNotEmpty()) liveMatches else dailyFallbackMatches

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
                .padding(top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ÉLŐ MÉRKŐZÉSEK",
                    color = LiveGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Eredmények valós időben",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black
                )
                if (lastUpdated.isNotBlank()) {
                    Text(
                        text = "Frissítve: $lastUpdated · automatikus frissítés 30 mp-enként",
                        color = LiveMuted,
                        fontSize = 10.sp
                    )
                }
            }
            TextButton(onClick = { onOpenSettings() }) {
                Text("⚙ API", color = LiveGreen)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "●",
                color = if (liveMatches.isNotEmpty()) LiveGreen else LiveMuted,
                fontSize = 12.sp
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = when {
                    liveMatches.isNotEmpty() -> "${liveMatches.size} élő mérkőzés"
                    showingDailyFallback -> "0 élő mérkőzés · Napi meccsek (offset=-1): ${displayedMatches.size}"
                    else -> "0 élő mérkőzés"
                },
                color = LiveMuted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = { scope.launch { refreshMatches() } },
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
        } else if (loading && displayedMatches.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 55.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LiveGreen)
            }
        } else if (displayedMatches.isEmpty()) {
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
                    Text("Most nincs élő mérkőzés", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "A lista automatikusan frissül.",
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
                items(
                    items = displayedMatches,
                    key = { "${it.id}_${it.homeTeam}_${it.awayTeam}_${it.startTime}" }
                ) { match ->
                    LiveMatchCard(match = match, isLive = liveMatches.isNotEmpty())
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.homeTeam,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = match.awayTeam,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    val badgeText = when {
                        match.status == "ÉLŐ" && match.startTime.isNotBlank() -> match.startTime
                        match.status.isNotBlank() -> match.status
                        match.startTime.isNotBlank() -> match.startTime
                        else -> "NAPI"
                    }
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "${match.homeScore}  :  ${match.awayScore}",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
