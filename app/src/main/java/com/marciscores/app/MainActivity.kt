
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

        setContent {
            MarciScoresApp()
        }
    }
}

private val DarkBackground = Color(0xFF070B18)
private val PanelColor = Color(0xFF111A2E)
private val NeonGreen = Color(0xFF20E875)
private val MutedText = Color(0xFF9EABC7)

@Composable
fun MarciScoresApp() {

    var showSettings by remember {
        mutableStateOf(false)
    }

    if (showSettings) {
        SettingsScreen(
            onBack = {
                showSettings = false
            }
        )
        return
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NeonGreen,
            background = DarkBackground,
            surface = PanelColor
        )
    ) {

        Scaffold(
            containerColor = DarkBackground,

            bottomBar = {

                NavigationBar(
                    containerColor = Color(0xFF0B1122)
                ) {

                    listOf(
                        "Főoldal",
                        "Élő",
                        "Elemzés",
                        "Bajnokságok"
                    ).forEach { item ->

                        NavigationBarItem(
                            selected = item == "Főoldal",

                            onClick = {},

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

                            label = {
                                Text(item, fontSize = 10.sp)
                            },

                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonGreen,
                                selectedTextColor = NeonGreen,
                                indicatorColor = Color(0xFF173B32),
                                unselectedIconColor = MutedText,
                                unselectedTextColor = MutedText
                            )
                        )
                    }
                }
            }

        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF102A50),
                                    Color(0xFF16113A),
                                    Color(0xFF092D2A)
                                )
                            )
                        )
                        .padding(18.dp),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            "MarciScores",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            "ÉLŐ FUTBALLKÖZPONT",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = {
                            showSettings = true
                        }
                    ) {

                        Text(
                            "⚙",
                            color = Color.White,
                            fontSize = 25.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Text(
                        "Szia, focirajongó! ⚽",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        "Kövesd a mai mérkőzéseket!",
                        color = MutedText,
                        fontSize = 14.sp
                    )

                    Spacer(
                        Modifier.height(20.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        colors = CardDefaults.cardColors(
                            containerColor = PanelColor
                        ),

                        shape = RoundedCornerShape(20.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),

                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                "● ÉLŐ EREDMÉNYEK",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Spacer(
                                Modifier.height(15.dp)
                            )

                            Text(
                                "⚽",
                                fontSize = 42.sp
                            )

                            Spacer(
                                Modifier.height(10.dp)
                            )

                            Text(
                                "Hamarosan érkeznek a meccsek!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Spacer(
                                Modifier.height(8.dp)
                            )

                            Text(
                                "Add meg a StatPal API-kulcsodat a beállításokban.",
                                color = MutedText,
                                fontSize = 12.sp
                            )

                            Spacer(
                                Modifier.height(15.dp)
                            )

                            Button(
                                onClick = {
                                    showSettings = true
                                },

                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color(0xFF07130D)
                                ),

                                shape = RoundedCornerShape(14.dp)
                            ) {

                                Text(
                                    "API beállítása",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(
                        Modifier.height(20.dp)
                    )

                    Text(
                        "FUTBALLKÖZPONT",
                        color = MutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    listOf(
                        "🏆" to "Bajnokságok",
                        "📊" to "Tabellák",
                        "🔥" to "Meccselemzések",
                        "⭐" to "Kedvenc csapatok"
                    ).forEach { (icon, title) ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),

                            colors = CardDefaults.cardColors(
                                containerColor = PanelColor
                            ),

                            shape = RoundedCornerShape(14.dp)
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp),

                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Text(
                                    icon,
                                    fontSize = 22.sp
                                )

                                Spacer(
                                    Modifier.width(12.dp)
                                )

                                Text(
                                    title,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(
                                    Modifier.weight(1f)
                                )

                                Text(
                                    "›",
                                    color = NeonGreen,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
