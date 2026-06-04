package com.dashboard

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
import androidx.compose.ui.Alignment
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

        lifecycleScope.launch {
            connectToAssettoCorsa("192.168.1.166") { speed, rpm, gear, isAbs, isTc ->
                speedState.floatValue = speed
                rpmState.floatValue = rpm
                gearState.value = gear
                absState.value = isAbs
                tcState.value = isTc
            }
        }

        setContent {
            MaterialTheme {
                DashboardScreen(
                    speed = speedState.floatValue,
                    rpm = rpmState.floatValue,
                    gear = gearState.value,
                    isAbsActive = absState.value,
                    isTcActive = tcState.value
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

@Composable
fun DashboardScreen(speed: Float, rpm: Float, gear: String, isAbsActive: Boolean, isTcActive: Boolean) {

    val minRpmLights = 5000f
    val shiftPoint = 6700f

    val isShiftLightActive = rpm >= shiftPoint
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkerTransition")

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    val dotColors = remember {
        listOf(
            Color(0xFF00FF00), Color(0xFF00FF00), Color(0xFF00FF00), Color(0xFF00FF00), Color(0xFF00FF00), // 1-5
            Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), Color(0xFFFF0000), // 6-10
            Color(0xFF0000FF), Color(0xFF0000FF), Color(0xFF0000FF), Color(0xFF0000FF), Color(0xFF0000FF)  // 11-15
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
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
                    val rpmPerDot = (shiftPoint - minRpmLights) / 15f

                    repeat(15) { index ->
                        val dotThreshold = minRpmLights + (index * rpmPerDot)
                        val isDotActive = rpm >= dotThreshold

                        val finalColor = when {
                            isShiftLightActive -> Color(0xFF0000FF).copy(alpha = blinkAlpha)
                            isDotActive -> dotColors[index]
                            else -> Color(0xFF2C2C2C)
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
                        .padding(horizontal = 8.dp, vertical = 16.dp),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gear
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = gear,
                        color = if (gear == "R") Color(0xFFFF0000) else Color.White,
                        fontSize = 170.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(175.dp)
                    )
                    Text(
                        text = "GEAR",
                        color = Color(0xFF888888),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Speed
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${speed.toInt()}",
                        color = Color.White,
                        fontSize = 110.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(115.dp)
                    )
                    Text(
                        text = "KM/H",
                        color = if (isShiftLightActive) Color.White else Color(0xFFFFB300),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
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
suspend fun connectToAssettoCorsa(pcIpAddress: String, onDataReceived: (Float, Float, String, Boolean, Boolean) -> Unit) {
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

                while (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
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

                        val gearDisplay = when (rawGear) {
                            0 -> "R"
                            1 -> "N"
                            else -> (rawGear - 1).toString()
                        }

                        withContext(Dispatchers.Main) {
                            if (speedKmh in 0f..450f && rpm in 0f..22000f) {
                                onDataReceived(speedKmh, rpm, gearDisplay, isAbsInAction, isTcInAction)
                            } else {
                                onDataReceived(speedKmh, if (rpm > 0f) rpm else 0f, gearDisplay, isAbsInAction, isTcInAction)
                            }
                        }
                    }
                } catch (_: java.net.SocketTimeoutException) {
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