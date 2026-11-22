package com.marciscores.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isDark: Boolean = true,
    onThemeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var apiKey by remember {
        mutableStateOf("")
    }

    var showKey by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf("")
    }

    var isSaved by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        try {
            apiKey = withContext(Dispatchers.IO) {
                ApiKeyStorage.load(context)
            }
            isSaved = apiKey.isNotBlank()
        } catch (e: Exception) {
            message = "Nem sikerült betölteni a mentett kulcsot."
        }

        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B18))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            TextButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    "‹ Vissza",
                    color = Color(0xFF20E875),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                Modifier.weight(1f)
            )

            Text(
                "⚙",
                color = Color(0xFF9EABC7),
                fontSize = 25.sp
            )
        }

        Spacer(
            Modifier.height(16.dp)
        )

        Text(
            "API-beállítások",
            color = Color.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            "Kapcsold össze a MarciScores alkalmazást a StatPal szolgáltatással.",
            color = Color(0xFF9EABC7),
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(16.dp))

        // Téma váltó
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111A2E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Megjelenés", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isDark) "Sötét téma" else "Világos téma",
                        color = Color(0xFF9EABC7),
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = isDark,
                    onCheckedChange = { onThemeChange(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF20E875),
                        checkedTrackColor = Color(0xFF173B32)
                    )
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF111A2E)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                Modifier.padding(18.dp)
            ) {

                Text(
                    "STATPAL API",
                    color = Color(0xFF20E875),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = apiKey,

                    onValueChange = {
                        apiKey = it
                        message = ""
                    },

                    modifier = Modifier.fillMaxWidth(),

                    label = {
                        Text("API-kulcs")
                    },

                    placeholder = {
                        Text("Illeszd be a StatPal kulcsodat")
                    },

                    singleLine = true,

                    visualTransformation = if (showKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),

                    trailingIcon = {
                        TextButton(
                            onClick = {
                                showKey = !showKey
                            }
                        ) {
                            Text(
                                if (showKey) "Elrejt" else "Mutat",
                                color = Color(0xFF20E875)
                            )
                        }
                    },

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF20E875),
                        unfocusedBorderColor = Color(0xFF34415C),
                        focusedLabelColor = Color(0xFF20E875),
                        unfocusedLabelColor = Color(0xFF9EABC7),
                        cursorColor = Color(0xFF20E875),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(
                    Modifier.height(15.dp)
                )

                Button(
                    onClick = {
                        if (apiKey.isBlank()) {
                            isSaved = false
                            message = "Először írd be az API-kulcsot."
                        } else {
                            try {
                                ApiKeyStorage.save(
                                    context,
                                    apiKey
                                )

                                isSaved = true
                                message = "API-kulcs helyben elmentve."
                            } catch (e: Exception) {
                                isSaved = false
                                message = "A mentés nem sikerült."
                            }
                        }
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),

                    shape = RoundedCornerShape(14.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF20E875),
                        contentColor = Color(0xFF07130D)
                    )
                ) {

                    Text(
                        "Kulcs mentése",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.height(8.dp)
                )

                OutlinedButton(
                    onClick = {
                        try {
                            ApiKeyStorage.delete(context)

                            apiKey = ""
                            isSaved = false
                            message = "A mentett API-kulcs törölve."
                        } catch (e: Exception) {
                            isSaved = false
                            message = "A törlés nem sikerült."
                        }
                    },

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(14.dp)
                ) {

                    Text(
                        "Mentett kulcs törlése",
                        color = Color(0xFFFF6B83)
                    )
                }

                if (message.isNotBlank()) {

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        message,
                        color = if (isSaved) {
                            Color(0xFF20E875)
                        } else {
                            Color(0xFFFFB74D)
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(
            Modifier.height(18.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF102A24)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {

            Row(
                Modifier.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    "🔐",
                    fontSize = 24.sp
                )

                Spacer(
                    Modifier.width(12.dp)
                )

                Column {

                    Text(
                        "Titkosított helyi tárolás",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Text(
                        "A kulcsot az alkalmazás titkosítva tárolja a készülékeden.",
                        color = Color(0xFF9EABC7),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (isLoading) {
            Spacer(
                Modifier.height(15.dp)
            )

            CircularProgressIndicator(
                color = Color(0xFF20E875)
            )
        }
    }
}
