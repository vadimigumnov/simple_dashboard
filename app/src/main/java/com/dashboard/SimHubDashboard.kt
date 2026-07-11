package com.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val maxRpm = viewModel.maxRpmState.floatValue
    val rpmValue = viewModel.rpmState.floatValue
    val currentSpeed = viewModel.speedState.floatValue
    val absState = viewModel.absState.value
    val tcState = viewModel.tcState.value
    val currentGear = viewModel.gearState.value
    val currentLapTime = viewModel.time0State.intValue
    val lastLapTime = viewModel.time1State.intValue
    val bestLapTime = viewModel.time2State.intValue

    val minRpmLights = maxRpm * 0.85f
    val shiftPoint = maxRpm * 0.9f
    val warningPoint = maxRpm * 0.95f
    val isWarningLightActive = rpmValue >= warningPoint
    val isShiftLightActive = rpmValue >= shiftPoint
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
            Color(0xFF00FF00),
            Color(0xFF00FF00),
            Color(0xFF00FF00),
            Color(0xFF00FF00),
            Color(0xFF00FF00),
            Color(0xFF00FF00), // 1-6
            Color(0xFFFF0000),
            Color(0xFFFF0000),
            Color(0xFFFF0000),
            Color(0xFFFF0000),
            Color(0xFFFF0000),
            Color(0xFFFF0000), // 7-12
            Color(0xFF0000FF),
            Color(0xFF0000FF),
            Color(0xFF0000FF),
            Color(0xFF0000FF) // 13-16
        )
    }

    val bgColor = when {
        isWarningLightActive -> Color(0xFFFF0000).copy(alpha = blinkAlpha)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = CenterVertically
                ) {
                    val rpmPerDot = (shiftPoint - minRpmLights) / 16f

                    repeat(16) { index ->
                        val dotThreshold = minRpmLights + (index * rpmPerDot)
                        val isDotActive = rpmValue >= dotThreshold

                        val finalColor = when {
                            isShiftLightActive -> Color(0xFF0000FF).copy(alpha = blinkAlpha)
                            isDotActive -> dotColors[index]
                            else -> Color(0xFF242424)
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(finalColor)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = CenterVertically
            ) {
                // Speed, ABS, TC, RPM
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IndicatorLight(text = "ABS", isActive = absState, activeColor = Color.Cyan)
                        IndicatorLight(text = "TC", isActive = tcState, activeColor = Color.Yellow)
                    }
                    Text(
                        text = "${currentSpeed.toInt()}",
                        color = Color.White,
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(130.dp)
                    )
                    Text(
                        text = "${rpmValue.toInt()} RPM",
                        color = if (isShiftLightActive) Color.White else Color(0xFF888888),
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.height(20.dp)
                    )
                }
                // Gear
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = currentGear,
                        color = if (currentGear == "R") Color(0xFFFF0000) else Color.White,
                        fontSize = 300.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center
                    )
                }
                // Lap Time
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatTime(currentLapTime.toLong()),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(44.dp)
                    )
                    Text(
                        text = formatTime(lastLapTime.toLong()),
                        color = if (lastLapTime == bestLapTime) Color.Green else Color(0xFFB83A1D),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(40.dp)
                    )
                    Text(
                        text = formatTime(bestLapTime.toLong()),
                        color = Color(0xFF7405A3),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.height(40.dp)
                    )
                }
            }
        }
    }
}