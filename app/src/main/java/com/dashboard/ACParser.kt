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

class ACParser {

    private var socket: DatagramSocket? = null
    private var isRunning = false

    suspend fun connectToAssettoCorsa(
        pcIpAddress: String,
        onDataReceived: (
            speed: Float,
            rpm: Float,
            maxRpm: Float,
            gear: String,
            isAbs: Boolean,
            isTc: Boolean,
            lapTime: Int,
            lapTimeBest: Int,
            lapTimeLast: Int,
            timeStamp: Long
        ) -> Unit
    ) = withContext(Dispatchers.IO) {

        val acPort = 9996
        isRunning = true

        try {
            socket = DatagramSocket()
            socket?.soTimeout = 1500

            val pcAddress = InetAddress.getByName(pcIpAddress)

            val handshakeJob = launch(Dispatchers.IO) {
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

                while (isRunning) {
                    try {
                        socket?.send(packetConnect)
                        delay(50)
                        socket?.send(packetUpdate)
                        Log.d("ACParser", "Handshake packets sent to AC...")
                    } catch (e: Exception) {
                        Log.e("ACParser", "Handshake error: ${e.message}")
                    }
                    delay(1000)
                }
            }

            val receiveBuffer = ByteArray(512)
            val incomingPacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            var maxRpm = 4000f

            while (isRunning) {
                try {
                    socket?.receive(incomingPacket)

                    if (incomingPacket.length == 328) {

                        val timeStamp = System.currentTimeMillis()
                        val buffer = ByteBuffer.wrap(incomingPacket.data, 0, incomingPacket.length)
                            .order(ByteOrder.LITTLE_ENDIAN)

                        val speedKmh = buffer.getFloat(8)
                        val rpm = buffer.getFloat(68)
                        if (rpm > maxRpm) {
                            maxRpm = rpm
                        }
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

                        val finalRpm = if (rpm > 0f && rpm < 22000f) rpm else 0f
                        val finalSpeed = if (speedKmh in 0f..450f) speedKmh else 0f

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
                } catch (_: SocketTimeoutException) {
                } catch (e: Exception) {
                    Log.e("ACParser", "Receive error: ${e.message}")
                    delay(100)
                }
            }

            handshakeJob.cancel()

        } catch (e: Exception) {
            Log.e("ACParser", "Critical error: ${e.message}")
        } finally {
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