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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LiveBg = Color(0xFF070B18)
private val LivePanel = Color(0xFF111A2E)
private val LiveGreen = Color(0xFF20E875)
private val LiveMuted = Color(0xFF9EABC7)

@Composable
fun LiveMatchesScreen(onOpenSettings: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val api = remember { StatPalApi() }
    val scope = rememberCoroutineScope()
    var matches by remember { mutableStateOf<List<LiveMatch>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf("") }

    suspend fun refreshMatches() {
        if (loading) return
        loading = true
        error = null
        try {
            val key = ApiKeyStorage.load(context)
            matches = api.getLiveMatches(key)
            val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            lastUpdated = time.format(java.util.Date())
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LiveBg)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ÉLŐ MÉRKŐZÉSEK", color = LiveGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Eredmények valós időben", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                if (lastUpdated.isNotBlank()) {
                    Text("Frissítve: $lastUpdated · automatikus frissítés 30 mp-enként", color = LiveMuted, fontSize = 10.sp)
                }
            }
            TextButton(onClick = { onOpenSettings() }) { Text("⚙ API", color = LiveGreen) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = LiveGreen, fontSize = 12.sp)
            Spacer(Modifier.width(7.dp))
            Text("${matches.size} élő mérkőzés", color = LiveMuted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { scope.launch { refreshMatches() } }, enabled = !loading) {
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
                    Button(onClick = onOpenSettings, colors = ButtonDefaults.buttonColors(containerColor = LiveGreen)) {
                        Text("API-kulcs beállítása", color = Color(0xFF07130D))
                    }
                }
            }
        } else if (loading && matches.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 55.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LiveGreen)
            }
        } else if (matches.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = LivePanel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚽", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Most nincs élő mérkőzés", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("A lista automatikusan frissül.", color = LiveMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matches, key = { it.id }) { match ->
                    LiveMatchCard(match)
                }
            }
        }
    }
}

@Composable
private fun LiveMatchCard(match: LiveMatch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LivePanel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                listOf(match.country, match.league).filter { it.isNotBlank() }.joinToString(" · "),
                color = LiveMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(match.homeTeam, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(match.awayTeam, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(match.status, color = LiveGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("${match.homeScore}  :  ${match.awayScore}", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
