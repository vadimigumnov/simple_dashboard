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

class GT7Parser {

    private var socket: DatagramSocket? = null
    private var isRunning = false

    suspend fun connectToGranTurismo7(
        ps4IpAddress: String,
        onDataReceived: (
            speed: Float,
            rpm: Float,
            maxRmp: Float,
            gear: String,
            isAbs: Boolean,
            isTc: Boolean,
            lapTime: Int,
            lapTimeBest: Int,
            lapTimeLast: Int,
            timeStamp: Long
        ) -> Unit
    ) = withContext(Dispatchers.IO) {

        val gt7HeartbeatPort = 33739
        val gt7ReceivePort = 33740

        var lastLapTimeLast = 0
        var currentLapStartTime = 0

        isRunning = true

        val sendFallbackData = {
            onDataReceived(
                0f,
                0f,
                0f,
                "--",
                false,
                false,
                -1,
                -1,
                -1,
                0L
            )
        }

        try {
            socket = DatagramSocket(gt7ReceivePort)
            socket?.soTimeout = 1500

            val ps4Address = InetAddress.getByName(ps4IpAddress)

            val handshakeJob = launch(Dispatchers.IO) {
                val heartbeatBuffer = ByteBuffer.allocate(1).apply { put(0x43) }
                val packetHeartbeat = DatagramPacket(
                    heartbeatBuffer.array(),
                    heartbeatBuffer.capacity(),
                    ps4Address,
                    gt7HeartbeatPort
                )

                while (isRunning) {
                    try {
                        socket?.send(packetHeartbeat)
                        Log.d("GT7Parser", "Heartbeat sent to GT7 (Port $gt7HeartbeatPort)...")
                    } catch (e: Exception) {
                        Log.e("GT7Parser", "Heartbeat error: ${e.message}")
                    }
                    delay(9000)
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
                    socket?.receive(incomingPacket)

                    val packetLength = incomingPacket.length
                    Log.d("GT7Parser", "Received packet! Size: $packetLength bytes")

                    if (packetLength > 0) {
                        val packetData = incomingPacket.data.copyOf(packetLength)

                        val decryptedData = decrypt.decryptGT7Packet(packetData)

                        if (decryptedData != null && decryptedData.size >= 368) {
                            val timeStamp = System.currentTimeMillis()

                            val buffer = ByteBuffer.wrap(decryptedData).order(ByteOrder.LITTLE_ENDIAN)
                            val magic = buffer.getInt(0)

                            if (magic == 0x47375330) {

                                // getting current game state
                                val flags = buffer.getShort(0x8E).toInt()
                                val isCarOnTrack = (flags and 1) != 0
                                val isPaused     = (flags and 2) != 0
                                val isLoading    = (flags and 4) != 0
                                // val isInGear     = (flags and 8) != 0
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
                                val isAbsInAction = (brakePhysical > 10) && ((brakePhysical - brakeActuated) > 10)

                                // preparing TC data
                                val throttleFiltered = buffer.get(0x91).toInt() and 0xFF
                                val throttlePhysical = buffer.get(0x13C).toInt() and 0xFF
                                val isTcInAction = (throttlePhysical - throttleFiltered) > 5

                                // preparing laptimes
                                val lapTimeBest = buffer.getInt(0x78)
                                val lapTimeLast = buffer.getInt(0x7C)
                                val currentTotalTime = buffer.getInt(0x80)
                                if (lapTimeLast != lastLapTimeLast) {
                                    lastLapTimeLast = lapTimeLast
                                    currentLapStartTime = currentTotalTime
                                }
                                val lapTime = currentTotalTime - currentLapStartTime

                                // sending prepared data
                                if (isRaceActive) {
                                    onDataReceived(
                                        finalSpeed,
                                        finalRpm,
                                        maxRpm,
                                        gearDisplay,
                                        isAbsInAction,
                                        isTcInAction,
                                        lapTime,
                                        lapTimeBest,
                                        lapTimeLast,
                                        timeStamp
                                    )
                                }
                            } else {
                                Log.w("GT7Parser", "Decrypted, but invalid magic signature: $magic")
                            }
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    sendFallbackData()
                } catch (e: Exception) {
                    Log.e("GT7Parser", "Receive error: ${e.message}")
                    delay(100)
                }
            }

            handshakeJob.cancel()

        } catch (e: Exception) {
            Log.e("GT7Parser", "Critical error: ${e.message}")
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