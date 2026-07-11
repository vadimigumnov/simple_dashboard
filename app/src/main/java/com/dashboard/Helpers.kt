package com.dashboard

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
fun formatTime(timeInMillis: Long): String {
    val milliseconds = timeInMillis % 1000
    val totalSeconds = timeInMillis / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60

    val returnTime = if (timeInMillis > 0) { String.format("%d:%02d:%03d", minutes, seconds, milliseconds) } else { String.format("--:--:--") }
    return returnTime
}