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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DrawDashboard {
    @Composable
    fun DashboardScreen(viewModel: DashboardViewModel) {
        val maxrpm = viewModel.maxRpmState.floatValue

        val minRpmLights = maxrpm * 0.7f
        val shiftPoint = maxrpm * 0.9f
        val warningPoint = maxrpm * 0.95f
        val isWarningLightActive = viewModel.rpmState.floatValue >= warningPoint
        val isShiftLightActive = viewModel.rpmState.floatValue >= shiftPoint
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
                            val isDotActive = viewModel.rpmState.floatValue >= dotThreshold

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
                            IndicatorLight(text = "ABS", isActive = viewModel.absState.value, activeColor = Color.Cyan)
                            IndicatorLight(text = "TC", isActive = viewModel.tcState.value, activeColor = Color.Yellow)
                        }

                        Text(
                            text = "${viewModel.rpmState.floatValue.toInt()} RPM",
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
                            text = viewModel.gearState.value,
                            color = if (viewModel.gearState.value == "R") Color(0xFFFF0000) else Color.White,
                            fontSize = 200.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.height(200.dp)
                        )
                    }

                    // Speed
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${viewModel.speedState.floatValue.toInt()}",
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
                            text = formatTime(viewModel.lapTimeState.intValue.toLong()),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.height(36.dp)
                        )
                        Text(
                            text = formatTime(viewModel.lapTimeLastState.intValue.toLong()),
                            color = if (viewModel.lapTimeLastState.intValue == viewModel.lapTimeBestState.intValue) Color.Green else Color.Yellow,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.height(26.dp)
                        )
                        Text(
                            text = formatTime(viewModel.lapTimeBestState.intValue.toLong()),
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
}