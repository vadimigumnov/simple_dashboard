package com.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onGameSelected: (GameType, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedGame by remember { mutableStateOf<GameType?>(null) }
    var ipAddress by remember { mutableStateOf("192.168.1.1") }
    var port by remember { mutableStateOf("2080") }

    LaunchedEffect(selectedGame) {
        selectedGame?.let {
            ipAddress = PreferenceManager.getIp(context, it)
            port = PreferenceManager.getPort(context, it)
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
                        onClick = { onGameSelected(GameType.GT7, "255.255.255.255", "0000") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0055BB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(240.dp, 60.dp)
                    ) {
                        Text(
                            "Gran Turismo 7",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { selectedGame = GameType.AC },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5500)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(240.dp, 60.dp)
                    ) {
                        Text(
                            "Assetto Corsa",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onGameSelected(GameType.RBR, "127.0.0.1", "6776") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF805500)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(240.dp, 60.dp)
                    ) {
                        Text(
                            "Richard Burns Rally",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { selectedGame = GameType.SIMHUB },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF036FFC)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(240.dp, 60.dp)
                    ) {
                        Text(
                            "SimHub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.width(320.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.weight(0.65f)
                        )

                        if (selectedGame != GameType.GT7) {
                            OutlinedTextField(
                                value = port,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 5) {
                                        port = it
                                    }
                                },
                                label = { Text("Port", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                modifier = Modifier.weight(0.35f)
                            )
                        }
                    }

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
                            onClick = {
                                selectedGame?.let {
                                    PreferenceManager.saveIp(context, it, ipAddress)
                                    PreferenceManager.savePort(context, it, port)
                                    onGameSelected(it, ipAddress, port)
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
    GT7, AC, RBR, SIMHUB
}