package com.dashboard

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.absoluteValue

class RBRParser {

    private var socket: DatagramSocket? = null
    private var isRunning = false

    suspend fun connectToRBR(
        pcIpAddress: String,
        onDataReceived: (
            speed: Float,
            rpm: Float,
            maxRpm: Float,
            gear: String,
            isAbs: Boolean,
            isTc: Boolean,
            time0: Int,
            time1: Int,
            time2: Int,
            param: Int,
            timeStamp: Long
        ) -> Unit
    ) = withContext(Dispatchers.IO) {

        val rbrPort = 6776
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
                -1,
                0L
            )
        }
        try {
            socket = DatagramSocket(rbrPort)
            socket?.soTimeout = 1500

            val receiveBuffer = ByteArray(664)
            val incomingPacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            var maxRpm = 4000f

            while (isRunning) {
                try {
                    socket?.receive(incomingPacket)

                    if (incomingPacket.length == 664) {
                        val timeStamp = System.currentTimeMillis()
                        val buffer = ByteBuffer.wrap(incomingPacket.data, 0, incomingPacket.length)
                            .order(ByteOrder.LITTLE_ENDIAN)

                        val speedKmh = buffer.getFloat(60).absoluteValue

                        val rpm = buffer.getFloat(136)
                        if (rpm > maxRpm) {
                            maxRpm = rpm
                        }

                        val rawGear = buffer.getInt(44)
                        val isAbsInAction = false
                        val isTcInAction = false
                        val time0 = (buffer.getFloat(12) * 1000).toInt()
                        val time1 = -1
                        val time2 = -1
                        val param = (buffer.getFloat(148)-273.15f).toInt() //engine temperature

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
                            time0,
                            time1,
                            time2,
                            param,
                            timeStamp
                        )
                    }
                } catch (_: SocketTimeoutException) {
                    maxRpm = 4000f
                    sendFallbackData()
                } catch (e: Exception) {
                    Log.e("RBRParser", "Receive error: ${e.message}")
                    delay(100)
                }
            }
        } catch (e: Exception) {
            Log.e("RBRParser", "Critical error: ${e.message}")
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