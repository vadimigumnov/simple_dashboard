package com.dashboard

import android.util.Log
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Salsa20 {

    companion object {
        private val bcProvider = BouncyCastleProvider()
        private val gt7Key = "Simulator Interface Packet GT7 v".toByteArray(Charsets.US_ASCII)
    }

    fun decryptGT7Packet(data: ByteArray): ByteArray? {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            val ivPart1 = buffer.getInt(0x40)

            val ivPart2 = ivPart1 xor 0xDEADBEAFL.toInt()

            val ivBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(ivPart2) //
                .putInt(ivPart1) //
                .array()

            val cipher = Cipher.getInstance("Salsa20", bcProvider)
            val keySpec = SecretKeySpec(gt7Key, "Salsa20")
            val ivSpec = IvParameterSpec(ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

            cipher.doFinal(data)

        } catch (e: Exception) {
            Log.e("GT7Parser", "Decryption failed: ${e.message}")
            null
        }
    }
}