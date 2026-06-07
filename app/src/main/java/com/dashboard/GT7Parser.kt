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
            maxrmp: Float,
            gear: String,
            isAbs: Boolean,
            isTc: Boolean,
            lapTime: Int,
            lapTimeBest: Int,
            lapTimeLast: Int
        ) -> Unit
    ) = withContext(Dispatchers.IO) {

        val gt7HeartbeatPort = 33739
        val gt7ReceivePort = 33740

        var lastLapTimeLast = 0
        var currentLapStartTime = 0

        isRunning = true

        try {
            socket = DatagramSocket(gt7ReceivePort)
            socket?.soTimeout = 1500

            val ps4Address = InetAddress.getByName(ps4IpAddress)

            val handshakeJob = launch(Dispatchers.IO) {
                val heartbeatBuffer = ByteBuffer.allocate(1).apply { put(0x41) }
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

            var maxRpm = 5000f

            while (isRunning) {
                try {
                    socket?.receive(incomingPacket)

                    val packetLength = incomingPacket.length
                    Log.d("GT7Parser", "Received packet! Size: $packetLength bytes")

                    if (packetLength > 0) {
                        val packetData = incomingPacket.data.copyOf(packetLength)

                        val decryptedData = decrypt.decryptGT7Packet(packetData)

                        if (decryptedData != null && decryptedData.size >= 296) {
                            val buffer = ByteBuffer.wrap(decryptedData).order(ByteOrder.LITTLE_ENDIAN)
                            val magic = buffer.getInt(0)

                            if (magic == 0x47375330) {

                                val speedMs = buffer.getFloat(0x4C)
                                val rpm = buffer.getFloat(0x3C)
                                if (rpm > maxRpm) {
                                    maxRpm = rpm
                                }

                                val rawGearInfo = buffer.get(0x90).toInt()
                                val currentGear = rawGearInfo and 0x0F

                                val isTcInAction = false
                                val isAbsInAction = false

                                val lapTimeBest = buffer.getInt(0x78)
                                val lapTimeLast = buffer.getInt(0x7C)

                                val currentTotalTime = buffer.getInt(0x80)
                                if (lapTimeLast != lastLapTimeLast) {
                                    lastLapTimeLast = lapTimeLast
                                    currentLapStartTime = currentTotalTime
                                }

                                val lapTime = currentTotalTime - currentLapStartTime

                                Log.w("GT7Parser", "laptime: $lapTime")
                                val gearDisplay = when (currentGear) {
                                    0 -> "R"
                                    15 -> "N"
                                    else -> currentGear.toString()
                                }

                                val finalRpm = if (rpm > 0f) rpm else 0f
                                val finalSpeed = if (speedMs > 0f) speedMs * 3.6f else 0f

                                onDataReceived(
                                    finalSpeed,
                                    finalRpm,
                                    maxRpm,
                                    gearDisplay,
                                    isAbsInAction,
                                    isTcInAction,
                                    lapTime,
                                    lapTimeBest,
                                    lapTimeLast
                                )
                            } else {
                                Log.w("GT7Parser", "Decrypted, but invalid magic signature: $magic")
                            }
                        }
                    }
                } catch (_: SocketTimeoutException) {
                } catch (e: Exception) {
                    Log.e("GT7Parser", "Receive error: ${e.message}")
                    delay(100)
                }
            }

            handshakeJob.cancel()

        } catch (e: Exception) {
            Log.e("GT7Parser", "Critical error: ${e.message}")
        } finally {
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