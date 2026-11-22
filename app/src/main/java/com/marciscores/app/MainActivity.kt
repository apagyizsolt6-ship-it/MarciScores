
package com.marciscores.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val Background = Color(0xFF070B18)
private val Panel = Color(0xFF111A2E)
private val Green = Color(0xFF20E875)
private val Blue = Color(0xFF168BFF)
private val Purple = Color(0xFF9A45FF)
private val Orange = Color(0xFFFFA51F)
private val TextMain = Color(0xFFF4F7FF)
private val TextMuted = Color(0xFF9EABC7)

@Composable
fun MarciScoresApp() {

    var selectedDate by remember {
        mutableStateOf("Ma")
    }

    var selectedTab by remember {
        mutableStateOf("Főoldal")
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Green,
            background = Background,
            surface = Panel,
            onBackground = TextMain,
            onSurface = TextMain
        )
    ) {

        Scaffold(
            containerColor = Background,

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
                            selected = selectedTab == item,

                            onClick = {
                                selectedTab = item
                            },

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
                                Text(
                                    item,
                                    fontSize = 10.sp
                                )
                            },

                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green,
                                selectedTextColor = Green,
                                indicatorColor = Color(0xFF173B32),
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
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
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                Header()

                DateSelector(
                    selected = selectedDate,
                    onSelect = {
                        selectedDate = it
                    }
                )

                LivePanel()

                QuickActions()

                LeaguePreview()

                ApiStatus()

                Spacer(
                    Modifier.height(18.dp)
                )
            }
        }
    }
}

@Composable
private fun Header() {

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
            .padding(
                horizontal = 18.dp,
                vertical = 22.dp
            ),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            Modifier.weight(1f)
        ) {

            Text(
                "MarciScores",
                fontSize = 29.sp,
                fontWeight = FontWeight.Black,
                color = TextMain
            )

            Text(
                "ÉLŐ FUTBALLKÖZPONT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Green
            )

            Text(
                "Foci • Eredmények • Statisztikák",
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Text(
            "⚽",
            fontSize = 42.sp
        )
    }
}

@Composable
private fun DateSelector(
    selected: String,
    onSelect: (String) -> Unit
) {

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            ),

        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        listOf(
            "Ma",
            "Holnap",
            "Holnapután"
        ).forEach { day ->

            val active = selected == day

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),

                shape = RoundedCornerShape(14.dp),

                color = if (active) {
                    Color(0xFF123C2C)
                } else {
                    Panel
                },

                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (active) Green else Color(0xFF273451)
                ),

                onClick = {
                    onSelect(day)
                }
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        day,
                        color = if (active) Green else TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LivePanel() {

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .border(
                1.dp,
                Color(0xFF284A76),
                RoundedCornerShape(18.dp)
            )
            .background(
                Panel,
                RoundedCornerShape(18.dp)
            )
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF162642),
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp
                    )
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                "●",
                color = Color(0xFFFF4C65),
                fontSize = 12.sp
            )

            Spacer(
                Modifier.width(8.dp)
            )

            Text(
                "ÉLŐ EREDMÉNYEK",
                color = TextMain,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )

            Spacer(
                Modifier.weight(1f)
            )

            Text(
                "0 meccs",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "⚽",
                fontSize = 35.sp
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                "Élő meccsek betöltése",
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(
                Modifier.height(5.dp)
            )

            Text(
                "A StatPal API csatlakoztatása után " +
                    "itt jelennek meg a valódi élő mérkőzések.",

                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun QuickActions() {

    val actions = listOf(
        "Bajnokságok" to Green,
        "Tabellák" to Blue,
        "Statisztikák" to Purple,
        "Kedvencek" to Color(0xFFFF3D8B)
    )

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 14.dp
            ),

        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        actions.forEach { (title, color) ->

            Column(
                Modifier
                    .weight(1f)
                    .height(78.dp)
                    .border(
                        1.dp,
                        color.copy(alpha = .75f),
                        RoundedCornerShape(15.dp)
                    )
                    .background(
                        color.copy(alpha = .13f),
                        RoundedCornerShape(15.dp)
                    )
                    .padding(7.dp),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    when (title) {
                        "Bajnokságok" -> "🏆"
                        "Tabellák" -> "📊"
                        "Statisztikák" -> "▥"
                        else -> "★"
                    },

                    fontSize = 20.sp
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    title,
                    color = TextMain,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LeaguePreview() {

    Column(
        Modifier.padding(horizontal = 12.dp)
    ) {

        Text(
            "NÉPSZERŰ BAJNOKSÁGOK",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        listOf(
            "🏴" to "Premier League",
            "🇪🇸" to "La Liga",
            "🇩🇪" to "Bundesliga",
            "🇮🇹" to "Serie A",
            "🇫🇷" to "Ligue 1"
        ).forEachIndexed { index, (flag, league) ->

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(
                        Panel,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    flag,
                    fontSize = 19.sp
                )

                Spacer(
                    Modifier.width(12.dp)
                )

                Text(
                    league,
                    color = TextMain,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Spacer(
                    Modifier.weight(1f)
                )

                Text(
                    "›",
                    color = listOf(
                        Green,
                        Blue,
                        Purple,
                        Orange,
                        Color(0xFFFF3D8B)
                    )[index],

                    fontSize = 23.sp
                )
            }
        }
    }
}

@Composable
private fun ApiStatus() {

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
            .background(
                Color(0xFF101A2B),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            "●",
            color = if (
                BuildConfig.STATPAL_API_KEY.isBlank()
            ) {
                Orange
            } else {
                Green
            },

            fontSize = 11.sp
        )

        Spacer(
            Modifier.width(8.dp)
        )

        Column {

            Text(
                "StatPal API",
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Text(
                if (
                    BuildConfig.STATPAL_API_KEY.isBlank()
                ) {
                    "API-kulcs még nincs beállítva"
                } else {
                    "API-kulcs konfigurálva"
                },

                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}
