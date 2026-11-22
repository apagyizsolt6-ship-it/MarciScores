package com.marciscores.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MarciScoresApp() }
    }
}

private val AppBackground = Color(0xFF070B18)
private val AppPanel = Color(0xFF111A2E)
private val AppGreen = Color(0xFF20E875)
private val AppMuted = Color(0xFF9EABC7)
private val AppAmber = Color(0xFFFFB84D)
private val AppPink = Color(0xFFFF5C8A)
private val AppBlue = Color(0xFF4DA6FF)

@Composable
fun MarciScoresApp() {
    val context = LocalContext.current
    var isDark by remember { mutableStateOf(ThemeStorage.isDark(context)) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Főoldal") }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = AppGreen,
            background = AppBackground,
            surface = AppPanel
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00A152),
            background = Color(0xFFF5F7FA),
            surface = Color.White
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        if (showSettings) {
            SettingsScreen(
                onBack = { showSettings = false },
                isDark = isDark,
                onThemeChange = { newDark ->
                    isDark = newDark
                    ThemeStorage.setDark(context, newDark)
                }
            )
        } else {
            Scaffold(
                containerColor = AppBackground,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF0B1122)) {
                        listOf("Főoldal", "Élő", "Tabellák", "Bajnokságok").forEach { item ->
                            NavigationBarItem(
                                selected = selectedTab == item,
                                onClick = { selectedTab = item },
                                icon = {
                                    Text(
                                        when (item) {
                                            "Főoldal" -> "⌂"
                                            "Élő" -> "●"
                                            "Tabellák" -> "📊"
                                            else -> "🏆"
                                        },
                                        fontSize = 19.sp
                                    )
                                },
                                label = { Text(item, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppGreen,
                                    selectedTextColor = AppGreen,
                                    indicatorColor = Color(0xFF173B32),
                                    unselectedIconColor = AppMuted,
                                    unselectedTextColor = AppMuted
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (selectedTab) {
                        "Élő" -> LiveMatchesScreen(onOpenSettings = { showSettings = true })
                        "Főoldal" -> HomeScreen(
                            onOpenSettings = { showSettings = true },
                            onOpenLive = { selectedTab = "Élő" },
                            onOpenTab = { selectedTab = it }
                        )
                        "Tabellák" -> StandingsScreen(onOpenSettings = { showSettings = true })
                        else -> ComingSoonScreen(
                            title = selectedTab,
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenTab: (String) -> Unit
) {
    val context = LocalContext.current
    var liveCount by remember { mutableIntStateOf(0) }
    var favorites by remember { mutableStateOf(FavoritesStorage.getFavorites(context)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            while (true) {
                try {
                    val key = ApiKeyStorage.load(context)
                    if (key.isNotBlank()) {
                        val api = StatPalApi()
                        liveCount = api.getLiveMatches(key).size
                    }
                } catch (_: Exception) {}
                delay(60_000)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1A237E),
                            Color(0xFF0D47A1),
                            Color(0xFF00695C),
                            Color(0xFF1B5E20)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MarciScores", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("ÉLŐ FUTBALLKÖZPONT", color = AppGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onOpenSettings) {
                    Text("⚙", color = Color.White, fontSize = 26.sp)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Szia, focirajongó! ⚽", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (liveCount > 0) "Most $liveCount élő mérkőzés zajlik világszerte!" else "Kövesd a mai mérkőzéseket!",
                color = AppMuted,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenLive() },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00C853), Color(0xFF00E676), Color(0xFF69F0AE))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("● ÉLŐ EREDMÉNYEK", color = Color(0xFF07130D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("⚽", fontSize = 40.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Nézd meg a most zajló meccseket!",
                            color = Color(0xFF07130D),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        if (liveCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text("$liveCount élő meccs", color = Color(0xFF1B5E20), fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (favorites.isNotEmpty()) {
                Text("⭐ KEDVENCEID", color = AppAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(favorites.toList()) { team ->
                        AssistChip(
                            onClick = { onOpenLive() },
                            label = { Text(team, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = AppPanel,
                                labelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("FUTBALLKÖZPONT", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            val menuItems = listOf(
                Triple("🏆", "Bajnokságok", AppBlue),
                Triple("📊", "Tabellák", AppGreen),
                Triple("🔥", "Meccselemzések", AppPink),
                Triple("⭐", "Kedvenc csapatok", AppAmber)
            )

            menuItems.forEach { (icon, title, accent) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable {
                            when (title) {
                                "Tabellák" -> onOpenTab("Tabellák")
                                "Bajnokságok" -> onOpenTab("Bajnokságok")
                                else -> onOpenLive()
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = AppPanel),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icon, fontSize = 24.sp)
                        Spacer(Modifier.width(14.dp))
                        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text("›", color = accent, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(title: String, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenSettings) { Text("⚙ API", color = AppGreen) }
        }
        Spacer(Modifier.height(35.dp))
        Text("🚧", fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text("Ez a rész fejlesztés alatt áll", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("A következő frissítésekben érkezik.", color = AppMuted, fontSize = 13.sp)
    }
}
