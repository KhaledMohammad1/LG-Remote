package com.example.lgremote.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLan {
    /**
     * Sends a Magic Packet to wake up LG TV over Wi-Fi / Ethernet.
     * Requires "Mobile TV On / Turn on via Wi-Fi" enabled in LG TV settings.
     */
    suspend fun wake(macAddress: String, broadcastIp: String = "255.255.255.255", port: Int = 9) = withContext(Dispatchers.IO) {
        try {
            val cleanMac = macAddress.replace(":", "").replace("-", "")
            if (cleanMac.length != 12) return@withContext false

            val macBytes = ByteArray(6)
            for (i in 0 until 6) {
                macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }

            // 6 bytes of 0xFF followed by 16 iterations of target MAC
            val bytes = ByteArray(6 + 16 * 6)
            for (i in 0 until 6) {
                bytes[i] = 0xFF.toByte()
            }
            var offset = 6
            for (i in 0 until 16) {
                System.arraycopy(macBytes, 0, bytes, offset, 6)
                offset += 6
            }

            val address = InetAddress.getByName(broadcastIp)
            val packet = DatagramPacket(bytes, bytes.size, address, port)
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(packet)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
