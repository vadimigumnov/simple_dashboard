package com.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartupMenu(
    onGameSelected: (GameType, String) -> Unit
) {
    val context = LocalContext.current
    var selectedGame by remember { mutableStateOf<GameType?>(null) }
    var ipAddress by remember { mutableStateOf("192.168.1.1") }

    LaunchedEffect(selectedGame) {
        selectedGame?.let {
            ipAddress = PreferenceManager.getIp(context, it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "DASHBOARD",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black
            )

            if (selectedGame == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { selectedGame = GameType.GT7 },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0055BB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(180.dp, 60.dp)
                    ) {
                        Text("Gran Turismo 7", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { selectedGame = GameType.AC },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5500)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(180.dp, 60.dp)
                    ) {
                        Text("Assetto Corsa", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        //onClick = { selectedGame = GameType.RBR },
                        onClick = {onGameSelected(GameType.RBR, "192.168.1.1")},
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805500)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(240.dp, 60.dp)
                    ) {
                        Text("Richard Burns Rally", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.width(320.dp)
                ) {
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() || char == '.' }) {
                                ipAddress = it
                            }
                        },
                        label = { Text("IP Address", color = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color(0xFFFF5500),
                            unfocusedIndicatorColor = Color.Gray,
                            focusedLabelColor = Color(0xFFFF5500),
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { selectedGame = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back", color = Color.White)
                        }

                        Button(
                            onClick = { selectedGame?.let {
                                PreferenceManager.saveIp(context, it, ipAddress)
                                onGameSelected(it, ipAddress)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5500)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

enum class GameType {
    GT7, AC, RBR
}