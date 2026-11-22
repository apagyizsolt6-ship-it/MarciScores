package com.marciscores.app

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val StandingBg = Color(0xFF070B18)
private val StandingPanel = Color(0xFF111A2E)
private val StandingGreen = Color(0xFF20E875)
private val StandingMuted = Color(0xFF9EABC7)
private val StandingGold = Color(0xFFFFD700)
private val StandingSilver = Color(0xFFC0C0C0)
private val StandingBronze = Color(0xFFCD7F32)

@Composable
fun StandingsScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val api = remember { StatPalApi() }
    val scope = rememberCoroutineScope()

    var leagueId by remember { mutableStateOf("39") } // Premier League alapból
    var standings by remember { mutableStateOf<List<LeagueStandingRow>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var leagueName by remember { mutableStateOf("Premier League") }

    fun loadStandings() {
        scope.launch {
            loading = true
            error = null
            try {
                val key = ApiKeyStorage.load(context)
                if (key.isBlank()) {
                    error = "Nincs API kulcs beállítva."
                    return@launch
                }
                standings = api.getLeagueStandings(key, leagueId)
                if (standings.isEmpty()) {
                    error = "Nem található tabella ehhez a ligához."
                }
            } catch (e: Exception) {
                error = e.message ?: "Hiba történt"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadStandings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StandingBg)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📊 Tabellák", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenSettings) { Text("⚙ API", color = StandingGreen) }
        }

        Spacer(Modifier.height(12.dp))

        // Liga választó (néhány népszerű)
        val popularLeagues = listOf(
            "39" to "Premier League",
            "140" to "La Liga",
            "135" to "Serie A",
            "78" to "Bundesliga",
            "61" to "Ligue 1",
            "2" to "Champions League",
            "3" to "Europa League"
        )

        Text("Liga:", color = StandingMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(popularLeagues) { (id, name) ->
                FilterChip(
                    selected = leagueId == id,
                    onClick = {
                        leagueId = id
                        leagueName = name
                        loadStandings()
                    },
                    label = { Text(name, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StandingGreen,
                        selectedLabelColor = Color(0xFF07130D)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StandingGreen)
            }
        } else if (error != null) {
            Text(error!!, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { loadStandings() }) { Text("Újrapróbálás") }
        } else {
            Text(leagueName, color = StandingGreen, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Fejléc
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StandingPanel, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 10.dp)
            ) {
                Text("#", color = StandingMuted, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                Text("Csapat", color = StandingMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("M", color = StandingMuted, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("GY", color = StandingMuted, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("D", color = StandingMuted, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("V", color = StandingMuted, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("GK", color = StandingMuted, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text("P", color = StandingMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }

            Spacer(Modifier.height(6.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(standings) { row ->
                    val pos = row.position.toIntOrNull() ?: 99
                    val posColor = when (pos) {
                        1 -> StandingGold
                        2 -> StandingSilver
                        3 -> StandingBronze
                        else -> Color.White
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StandingPanel.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(row.position, color = posColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(28.dp))
                        Text(row.teamName, color = Color.White, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        Text(row.played, color = StandingMuted, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Text(row.won, color = StandingMuted, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Text(row.drawn, color = StandingMuted, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Text(row.lost, color = StandingMuted, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Text(row.goalDifference, color = StandingMuted, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        Text(row.points, color = StandingGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}
