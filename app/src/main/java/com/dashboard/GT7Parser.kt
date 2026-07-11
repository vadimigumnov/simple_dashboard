package com.dashboard

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds

class GT7Parser {

    private var socket: DatagramSocket? = null
    private var isRunning = false

    suspend fun connectToGranTurismo7(
        ps4IpAddress: String,
        ps4Port: String,
        onDataReceived: (
            speed: Float, rpm: Float, maxRmp: Float, gear: String,
            isAbs: Boolean, isTc: Boolean, time0: Int, time1: Int,
            time2: Int, param: Int, timeStamp: Long
        ) -> Unit
    ) = withContext(Dispatchers.IO) {

        val gt7HeartbeatPort = 33739
        val gt7ReceivePort = 33740

        var lastLapTimeLast = 0
        var currentLapStartTime = 0

        isRunning = true

        val sendFallbackData = {
            onDataReceived(0f, 0f, 0f, "--", false, false, -1, -1, -1, -1, 0L)
        }

        try {
            socket = DatagramSocket(gt7ReceivePort)
            socket?.soTimeout = 1500

            socket?.broadcast = true

            val isAutoDiscovery = ps4IpAddress.isEmpty() || ps4IpAddress.equals(
                "auto",
                ignoreCase = true
            ) || ps4IpAddress == "255.255.255.255"

            val currentTargetAddress =
                AtomicReference(InetAddress.getByName(if (isAutoDiscovery) "255.255.255.255" else ps4IpAddress))

            val handshakeJob = launch(Dispatchers.IO) {
                val heartbeatBuffer = ByteBuffer.allocate(1).apply { put(0x43) }

                while (isRunning) {
                    try {
                        val packetHeartbeat = DatagramPacket(
                            heartbeatBuffer.array(),
                            heartbeatBuffer.capacity(),
                            currentTargetAddress.get(),
                            gt7HeartbeatPort
                        )
                        socket?.send(packetHeartbeat)
                        Log.d(
                            "GT7Parser",
                            "Heartbeat sent to ${currentTargetAddress.get().hostAddress}:$gt7HeartbeatPort..."
                        )
                    } catch (e: Exception) {
                        Log.e("GT7Parser", "Heartbeat error: ${e.message}")
                    }

                    val currentDelay =
                        if (currentTargetAddress.get().hostAddress == "255.255.255.255") 1000L else 9000L
                    delay(currentDelay.milliseconds)
                }
            }

            val decrypt = Salsa20()
            val receiveBuffer = ByteArray(2048)
            val incomingPacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            Log.d("GT7Parser", "Listening for Gran Turismo 7 on port $gt7ReceivePort...")

            var maxRpm = 4000f
            var lastCarId = -1

            while (isRunning) {
                try {
                    incomingPacket.length = receiveBuffer.size

                    socket?.receive(incomingPacket)

                    val packetLength = incomingPacket.length

                    if (packetLength > 0) {
                        val packetData = incomingPacket.data.copyOf(packetLength)
                        val decryptedData = decrypt.decryptGT7Packet(packetData)

                        if (decryptedData != null && decryptedData.size >= 368) {
                            val timeStamp = System.currentTimeMillis()
                            val buffer =
                                ByteBuffer.wrap(decryptedData).order(ByteOrder.LITTLE_ENDIAN)
                            val magic = buffer.getInt(0)

                            if (magic == 0x47375330) {

                                if (currentTargetAddress.get().hostAddress == "255.255.255.255") {
                                    currentTargetAddress.set(incomingPacket.address)
                                    Log.i(
                                        "GT7Parser",
                                        "✅ Auto-discovery successful! Locked onto PlayStation at ${currentTargetAddress.get().hostAddress}"
                                    )
                                }

                                // getting current game state
                                val flags = buffer.getShort(0x8E).toInt()
                                val isCarOnTrack = (flags and 1) != 0
                                val isPaused = (flags and 2) != 0
                                val isLoading = (flags and 4) != 0
                                val isRaceActive = isCarOnTrack && !isPaused && !isLoading

                                // calibrate maxRpm and checking if car was changed
                                val currentCarId = buffer.getInt(0x124)
                                if (currentCarId != lastCarId) {
                                    lastCarId = currentCarId
                                    maxRpm = 4000f
                                }
                                val rpm = buffer.getFloat(0x3C)
                                if (rpm > maxRpm) {
                                    maxRpm = rpm
                                }
                                val finalRpm = if (rpm > 0f) rpm else 0f

                                // gear and speed data
                                val speedMs = buffer.getFloat(0x4C)
                                val finalSpeed = if (speedMs > 0f) speedMs * 3.6f else 0f
                                val rawGearInfo = buffer.get(0x90).toInt()
                                val gearDisplay = when (val currentGear = rawGearInfo and 0x0F) {
                                    0 -> "R"
                                    15 -> "N"
                                    else -> currentGear.toString()
                                }

                                // preparing ABS data
                                val brakePhysical = buffer.get(0x92).toInt() and 0xFF
                                val brakeActuated = buffer.get(0x13D).toInt() and 0xFF
                                val isAbsInAction =
                                    (brakePhysical > 10) && ((brakePhysical - brakeActuated) > 10)

                                // preparing TC data
                                val throttleFiltered = buffer.get(0x91).toInt() and 0xFF
                                val throttlePhysical = buffer.get(0x13C).toInt() and 0xFF
                                val isTcInAction = (throttlePhysical - throttleFiltered) > 5

                                // preparing laptimes
                                val time2 = buffer.getInt(0x78)
                                val time1 = buffer.getInt(0x7C)
                                val currentTotalTime = buffer.getInt(0x80)
                                if (time1 != lastLapTimeLast) {
                                    lastLapTimeLast = time1
                                    currentLapStartTime = currentTotalTime
                                }
                                val time0 = currentTotalTime - currentLapStartTime
                                val param = -1

                                // sending prepared data
                                if (isRaceActive) {
                                    onDataReceived(
                                        finalSpeed,
                                        finalRpm,
                                        maxRpm,
                                        gearDisplay,
                                        isAbsInAction,
                                        isTcInAction,
                                        time0,
                                        time1,
                                        time2,
                                        param,
                                        timeStamp
                                    )
                                } else {
                                    sendFallbackData()
                                }
                            } else {
                                Log.w("GT7Parser", "Decrypted, but invalid magic signature: $magic")
                                sendFallbackData()
                            }
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    sendFallbackData()
                } catch (e: Exception) {
                    Log.e("GT7Parser", "Receive error: ${e.message}")
                    delay(100.milliseconds)
                    sendFallbackData()
                }
            }
            handshakeJob.cancel()

        } catch (e: Exception) {
            Log.e("GT7Parser", "Critical error: ${e.message}")
            sendFallbackData()
        } finally {
            sendFallbackData()
            closeSocket()
        }
    }

    fun stop() {
        isRunning = false
        closeSocket()
    }

    private fun closeSocket() {
        socket?.let {
            if (!it.isClosed) it.close()
        }
        socket = null
    }
}