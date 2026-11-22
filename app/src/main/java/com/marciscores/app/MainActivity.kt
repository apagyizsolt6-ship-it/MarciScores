package com.marciscores.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun MarciScoresApp() {
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Főoldal") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppGreen,
            background = AppBackground,
            surface = AppPanel
        )
    ) {
        if (showSettings) {
            SettingsScreen(onBack = { showSettings = false })
        } else {
            Scaffold(
                containerColor = AppBackground,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF0B1122)) {
                        listOf("Főoldal", "Élő", "Elemzés", "Bajnokságok").forEach { item ->
                            NavigationBarItem(
                                selected = selectedTab == item,
                                onClick = { selectedTab = item },
                                icon = {
                                    Text(
                                        when (item) {
                                            "Főoldal" -> "⌂"
                                            "Élő" -> "●"
                                            "Elemzés" -> "▥"
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
                        "Főoldal" -> HomeScreen(onOpenSettings = { showSettings = true }, onOpenLive = { selectedTab = "Élő" })
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
private fun HomeScreen(onOpenSettings: () -> Unit, onOpenLive: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF102A50), Color(0xFF16113A), Color(0xFF092D2A))
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MarciScores", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("ÉLŐ FUTBALLKÖZPONT", color = AppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpenSettings) {
                Text("⚙", color = Color.White, fontSize = 25.sp)
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Szia, focirajongó! ⚽", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Kövesd a mai mérkőzéseket!", color = AppMuted, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppPanel),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("● ÉLŐ EREDMÉNYEK", color = AppGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(15.dp))
                    Text("⚽", fontSize = 42.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Nézd meg a most zajló meccseket!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Az élő eredmények a StatPal API-ból töltődnek.", color = AppMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(15.dp))
                    Button(
                        onClick = onOpenLive,
                        colors = ButtonDefaults.buttonColors(containerColor = AppGreen, contentColor = Color(0xFF07130D)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Élő mérkőzések", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("FUTBALLKÖZPONT", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            listOf("🏆" to "Bajnokságok", "📊" to "Tabellák", "🔥" to "Meccselemzések", "⭐" to "Kedvenc csapatok").forEach { (icon, title) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPanel),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icon, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("›", color = AppGreen, fontSize = 24.sp)
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
