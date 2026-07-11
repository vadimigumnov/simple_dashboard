package com.dashboard

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SimHubParser(
    private val targetIp: String,
    private val targetPort: String,
    private val onTelemetryReceived: (SimHubTelemetry) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var isRunning = false
    private var pollingThread: Thread? = null

    val sendFallbackData = {
        SimHubTelemetry(
            timeStamp = 0L,
            maxRpm = 0f,
            rpmValue = 0f,
            speed = 0f,
            absActive = false,
            tcActive = false,
            gear = "N",
            currentLapTime = -1,
            lastLapTime = -1,
            bestLapTime = -1
        )
    }

    fun parseTimeToMs(timeString: String?): Int {
        if (timeString.isNullOrEmpty() || timeString == "00:00:00") return 0

        return try {
            val parts = timeString.split(":")
            if (parts.size >= 3) {
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                val secondsParts = parts[2].split(".")
                val seconds = secondsParts[0].toInt()
                val msRaw = if (secondsParts.size > 1) {
                    secondsParts[1].take(3).padEnd(3, '0').toInt()
                } else {
                    0
                }

                (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + msRaw
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private var isHighRefreshRate = true
    private var lastFrameTimeStr = ""
    private var lastFrameTimeMs = 0L
    private var slowUpdatesDetectedCount = 0

    private fun parseFrameTimeToMs(timeStr: String): Long {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.parse(timeStr).toEpochMilli()
            } else {
                val format =
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                format.parse(timeStr)?.time ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true

        pollingThread = Thread {
            while (isRunning) {
                try {
                    val request = Request.Builder()
                        .url("http://$targetIp:$targetPort/api/getgamedata")
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val jsonData = response.body?.string() ?: ""
                        val json = JSONObject(jsonData)

                        val currentFrameTimeStr = json.optString("FrameTimeUTC", "")

                        val isGameRunning = json.optBoolean("GameRunning", false)
                        val isGameInRace = json.optBoolean("IsGameInRace", false)
                        val isGamePaused = json.optBoolean("GamePaused", false)
                        val isGameInMenu = json.optBoolean("GameInMenu", false)

                        if (!isGameRunning || !isGameInRace || isGamePaused || isGameInMenu) {
                            onTelemetryReceived(sendFallbackData())
                        } else {
                            val data = json.optJSONObject("NewData") ?: json

                            if (isHighRefreshRate) {
                                if (currentFrameTimeStr.isNotEmpty() && currentFrameTimeStr != lastFrameTimeStr) {
                                    val currentFrameMs = parseFrameTimeToMs(currentFrameTimeStr)

                                    if (lastFrameTimeMs > 0L) {
                                        val delta = currentFrameMs - lastFrameTimeMs

                                        if (delta in 80..120) {
                                            slowUpdatesDetectedCount++
                                            if (slowUpdatesDetectedCount > 4) {
                                                isHighRefreshRate = false
                                                Log.e(
                                                    "SimHubParser",
                                                    "Delta: $delta ms. Switching to 10Hz."
                                                )
                                            }
                                        } else if (delta < 80) {
                                            slowUpdatesDetectedCount = 0
                                        }
                                    }

                                    lastFrameTimeStr = currentFrameTimeStr
                                    lastFrameTimeMs = currentFrameMs
                                }
                            }

                            val telemetry = SimHubTelemetry(
                                timeStamp = System.currentTimeMillis(),
                                maxRpm = data.optDouble("MaxRpm", 8000.0).toFloat(),
                                rpmValue = data.optDouble("Rpms", 0.0).toFloat(),
                                speed = data.optDouble("SpeedKmh", 0.0).toFloat(),
                                absActive = data.optInt("ABSActive", 0) > 0,
                                tcActive = data.optInt("TCActive", 0) > 0,
                                gear = data.optString("Gear", "N"),
                                currentLapTime = parseTimeToMs(
                                    data.optString(
                                        "CurrentLapTime",
                                        "00:00:00"
                                    )
                                ),
                                lastLapTime = parseTimeToMs(
                                    data.optString(
                                        "LastLapTime",
                                        "00:00:00"
                                    )
                                ),
                                bestLapTime = parseTimeToMs(
                                    data.optString(
                                        "BestLapTime",
                                        "00:00:00"
                                    )
                                ),
                            )
                            onTelemetryReceived(telemetry)
                        }
                    } else {
                        Log.e("SimHubParser", "HTTP Error: ${response.code} ${response.message}")
                        onTelemetryReceived(sendFallbackData())
                    }
                    response.close()

                    Thread.sleep(if (isHighRefreshRate) 16 else 100)

                } catch (e: IOException) {
                    Log.e("SimHubParser", "Network Error: ${e.message}")
                    onTelemetryReceived(sendFallbackData())
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    Log.e("SimHubParser", "Parse Error: ${e.message}")
                    onTelemetryReceived(sendFallbackData())
                    Thread.sleep(1000)
                }
            }
        }
        pollingThread?.start()
    }

    fun stop() {
        isRunning = false
        pollingThread?.interrupt()
        pollingThread = null
    }
}