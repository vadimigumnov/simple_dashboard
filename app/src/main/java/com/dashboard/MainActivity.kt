package com.dashboard

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.compose.foundation.shape.CircleShape
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Job
import java.net.SocketTimeoutException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        hideSystemBars()

        val speedState = mutableFloatStateOf(0f)
        val rpmState = mutableFloatStateOf(0f)
        val gearState = mutableStateOf("N")
        val absState = mutableStateOf(false)
        val tcState = mutableStateOf(false)
        val lapTimeState = mutableIntStateOf(0)
        val lapTimeBestState = mutableIntStateOf(0)
        val lapTimeLastState = mutableIntStateOf(0)
        val targetIp = getSavedIp(this)

        lifecycleScope.launch {
            connectToAssettoCorsa(targetIp) { speed, rpm, gear, isAbs, isTc, lapTime, lapTimeBest, lapTimeLast ->
                speedState.floatValue = speed
                rpmState.floatValue = rpm
                gearState.value = gear
                absState.value = isAbs
                tcState.value = isTc
                lapTimeState.intValue = lapTime
                lapTimeBestState.intValue = lapTimeBest
                lapTimeLastState.intValue = lapTimeLast
            }
        }

        setContent {
            MaterialTheme {
                DashboardScreen(
                    speed = speedState.floatValue,
                    rpm = rpmState.floatValue,
                    gear = gearState.value,
                    isAbsActive = absState.value,
                    isTcActive = tcState.value,
                    lapTime = lapTimeState.intValue,
                    lapTimeBest = lapTimeBestState.intValue,
                    lapTimeLast = lapTimeLastState.intValue
                )
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }
}

fun getSavedIp(context: Context): String {
    val prefs = context.getSharedPreferences("dash_settings", Context.MODE_PRIVATE)
    return prefs.getString("target_ip", "192.168.1.166") ?: "192.168.1.166"
}

@SuppressLint("UseKtx")
fun saveIp(context: Context, ip: String) {
    val prefs = context.getSharedPreferences("dash_settings", Context.MODE_PRIVATE)
    prefs.edit().putString("target_ip", ip).apply()
}

@Composable
fun SettingsDialog(
    currentIp: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var ipInput by remember { mutableStateOf(currentIp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Settings") },
        text = {
            Column {
                Text(text = "IP address:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    placeholder = { Text("192.168.x.x") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(ipInput) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DashboardScreen(speed: Float, rpm: Float, gear: String, isAbsActive: Boolean, isTcActive: Boolean, lapTime: Int, lapTimeBest: Int, lapTimeLast: Int) {

    val minRpmLights = 5000f
    val shiftPoint = 6500f
    val warningPoint = 6900f
    val localContext = LocalContext.current
    val isWarningLightActive = rpm >= warningPoint
    val isShiftLightActive = rpm >= shiftPoint
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkerTransition")
    var showSettings by remember { mutableStateOf(true) }
    var currentIp by remember { mutableStateOf(getSavedIp(localContext)) }

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    val blinkWarning by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkWarning"
    )

    val dotColors = remember {
        listOf(
            Color(0xFF00FF00), Color(0xFF00FF00), Color(0xFF00FF00), Color(0xFF00FF00), Color(0xFF00FF00), Color(0xFF00FF00), // 1-6
            Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), // 7-12
            Color(0xFF0000FF), Color(0xFF0000FF), Color(0xFF0000FF), Color(0xFF0000FF) // 13-16
        )
    }
    if (showSettings) {
        SettingsDialog(
            currentIp = currentIp,
            onDismiss = { showSettings = false },
            onSave = { newIp ->
                saveIp(localContext, newIp)
                currentIp = newIp
                showSettings = false
            }
        )
    }

    val bgColor = when {
        isWarningLightActive -> Color(0xFFFF0000).copy(alpha = blinkWarning)
        else -> Color(0xFF2C2C2C)
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(timeInMillis: Long): String {
        val milliseconds = timeInMillis % 1000
        val totalSeconds = timeInMillis / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return String.format("%d:%02d:%03d", minutes, seconds, milliseconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(modifier = Modifier.fillMaxWidth()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val rpmPerDot = (shiftPoint - minRpmLights) / 16f

                    repeat(16) { index ->
                        val dotThreshold = minRpmLights + (index * rpmPerDot)
                        val isDotActive = rpm >= dotThreshold

                        val finalColor = when {
                            isShiftLightActive -> Color(0xFF0000FF).copy(alpha = blinkAlpha)
                            isDotActive -> dotColors[index]
                            else -> Color(0xFF888888)
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(finalColor)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IndicatorLight(text = "ABS", isActive = isAbsActive, activeColor = Color.Cyan)
                        IndicatorLight(text = "TC", isActive = isTcActive, activeColor = Color.Yellow)
                    }

                    Text(
                        text = "${rpm.toInt()} RPM",
                        color = if (isShiftLightActive) Color.White else Color(0xFF888888),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium
                    )
                }
            }


            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                // Gear
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = gear,
                        color = if (gear == "R") Color(0xFFFF0000) else Color.White,
                        fontSize = 200.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(200.dp)
                    )
                }

                // Speed
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${speed.toInt()}",
                        color = Color.White,
                        fontSize = 90.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(95.dp)
                    )
                    Text(
                        text = "KM/H",
                        color = if (isShiftLightActive) Color.White else Color(0xFFFFB300),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = formatTime(lapTime.toLong()),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(36.dp)
                    )
                    Text(
                        text = formatTime(lapTimeLast.toLong()),
                        color = if (lapTimeLast == lapTimeBest) Color.Green else Color.Yellow,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(26.dp)
                    )
                    Text(
                        text = formatTime(lapTimeBest.toLong()),
                        color = Color.Green,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IndicatorLight(text: String, isActive: Boolean, activeColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) activeColor else Color(0xFF2C2C2C))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color.Black else Color(0xFF555555),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
suspend fun connectToAssettoCorsa(pcIpAddress: String, onDataReceived: (Float, Float, String, Boolean, Boolean, Int, Int, Int) -> Unit) {
    withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 1500

            val pcAddress = InetAddress.getByName(pcIpAddress)
            val acPort = 9996

            launch {
                val bufferConnect = ByteBuffer.allocate(12).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                    putInt(1).putInt(1).putInt(0)
                }
                val bufferUpdate = ByteBuffer.allocate(12).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                    putInt(1).putInt(1).putInt(1)
                }
                val packetConnect = DatagramPacket(bufferConnect.array(), bufferConnect.capacity(), pcAddress, acPort)
                val packetUpdate = DatagramPacket(bufferUpdate.array(), bufferUpdate.capacity(), pcAddress, acPort)

                while (coroutineContext[Job]?.isActive == true) {
                    try {
                        socket.send(packetConnect)
                        delay(50)
                        socket.send(packetUpdate)
                    } catch (e: Exception) {
                        Log.e("AC_Telemetry", "Error: ${e.message}")
                    }
                    delay(1000)
                }
            }

            while (true) {
                try {
                    val receiveBuffer = ByteArray(512)
                    val incomingPacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

                    socket.receive(incomingPacket)

                    if (incomingPacket.length == 328) {
                        val buffer = ByteBuffer.wrap(incomingPacket.data, 0, incomingPacket.length).order(ByteOrder.LITTLE_ENDIAN)

                        val speedKmh = buffer.getFloat(8)
                        val rpm = buffer.getFloat(68)
                        val rawGear = buffer.getInt(76)
                        val isAbsInAction = buffer.get(21).toInt() == 1
                        val isTcInAction = buffer.get(22).toInt() == 1
                        val lapTime = buffer.getInt(40)
                        val lapTimeBest = buffer.getInt(48)
                        val lapTimeLast = buffer.getInt(44)

                        val gearDisplay = when (rawGear) {
                            0 -> "R"
                            1 -> "N"
                            else -> (rawGear - 1).toString()
                        }

                        withContext(Dispatchers.Main) {
                            if (speedKmh in 0f..450f && rpm in 0f..22000f) {
                                onDataReceived(speedKmh, rpm, gearDisplay, isAbsInAction, isTcInAction, lapTime, lapTimeBest, lapTimeLast)
                            } else {
                                onDataReceived(speedKmh, if (rpm > 0f) rpm else 0f, gearDisplay, isAbsInAction, isTcInAction, lapTime,  lapTimeBest, lapTimeLast)
                            }
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    // Timeout
                } catch (_: Exception) {
                    delay(100)
                }
            }
        } catch (_: Exception) {
            // Network fail
        } finally {
            socket?.close()
        }
    }
}