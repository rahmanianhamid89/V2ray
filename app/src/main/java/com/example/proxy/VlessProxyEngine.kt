package com.example.proxy

import com.example.logger.AppLogger
import com.example.model.ServerConfig
import com.example.model.TransportType
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.UUID
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class VlessProxyEngine(private val config: ServerConfig) {

    private var clientSocket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    fun connect(): Result<Unit> {
        return try {
            AppLogger.i("VlessProxyEngine", "Initiating VLESS socket connection to ${config.address}:${config.port}")

            val rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(config.address, config.port), 10000)

            val activeSocket = if (config.security == "tls" || config.security == "reality") {
                AppLogger.i("VlessProxyEngine", "Performing TLS Handshake (SNI: ${config.sni.ifBlank { config.address }})")
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket(rawSocket, config.address, config.port, true) as SSLSocket

                val params = SSLParameters()
                val sniHost = config.sni.ifBlank { config.address }
                if (sniHost.isNotBlank()) {
                    params.serverNames = listOf(SNIHostName(sniHost))
                }
                if (config.alpn.isNotBlank()) {
                    val alpnProtocols = config.alpn.split(",").map { it.trim() }.toTypedArray()
                    params.applicationProtocols = alpnProtocols
                }
                sslSocket.sslParameters = params
                sslSocket.startHandshake()
                sslSocket
            } else {
                rawSocket
            }

            clientSocket = activeSocket
            inputStream = activeSocket.getInputStream()
            outputStream = activeSocket.getOutputStream()

            if (config.transportType == TransportType.WEBSOCKET) {
                performWebSocketHandshake(outputStream!!, inputStream!!)
            }

            isConnected = true
            AppLogger.i("VlessProxyEngine", "VLESS socket tunnel established successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.e("VlessProxyEngine", "Failed to connect VLESS socket: ${e.localizedMessage}", e)
            close()
            Result.failure(e)
        }
    }

    private fun performWebSocketHandshake(out: OutputStream, inp: InputStream) {
        val host = config.wsHost.ifBlank { config.address }
        val path = config.wsPath.ifBlank { "/" }
        val req = "GET $path HTTP/1.1\r\n" +
                "Host: $host\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n"

        out.write(req.toByteArray(Charsets.UTF_8))
        out.flush()

        val reader = inp.bufferedReader()
        val firstLine = reader.readLine() ?: ""
        if (!firstLine.contains("101")) {
            throw IllegalStateException("WebSocket handshake failed. Response: $firstLine")
        }
        AppLogger.i("VlessProxyEngine", "WebSocket handshake HTTP 101 Switch Protocols succeeded.")
    }

    fun buildVlessHeader(targetHost: String, targetPort: Int, isUdp: Boolean = false): ByteArray {
        val uuidBytes = getUuidBytes(config.uuid)
        val hostBytes = targetHost.toByteArray(Charsets.UTF_8)

        val buffer = ByteBuffer.allocate(1 + 16 + 1 + 1 + 2 + 1 + 1 + hostBytes.size)
        // Protocol Version 0x00
        buffer.put(0x00.toByte())
        // UUID 16 bytes
        buffer.put(uuidBytes)
        // Addons length 0x00
        buffer.put(0x00.toByte())
        // Command 0x01 = TCP, 0x02 = UDP
        buffer.put(if (isUdp) 0x02.toByte() else 0x01.toByte())
        // Target Port (Big Endian 2 bytes)
        buffer.putShort(targetPort.toShort())
        // Address Type: 0x02 = Domain
        buffer.put(0x02.toByte())
        // Domain Length
        buffer.put(hostBytes.size.toByte())
        // Domain Bytes
        buffer.put(hostBytes)

        return buffer.array()
    }

    private fun getUuidBytes(uuidStr: String): ByteArray {
        return try {
            val uuid = UUID.fromString(uuidStr)
            val bb = ByteBuffer.allocate(16)
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)
            bb.array()
        } catch (e: Exception) {
            ByteArray(16)
        }
    }

    fun getOutputStream(): OutputStream? = outputStream
    fun getInputStream(): InputStream? = inputStream

    fun close() {
        isConnected = false
        try { inputStream?.close() } catch (ignored: Exception) {}
        try { outputStream?.close() } catch (ignored: Exception) {}
        try { clientSocket?.close() } catch (ignored: Exception) {}
        clientSocket = null
        inputStream = null
        outputStream = null
        AppLogger.i("VlessProxyEngine", "VLESS socket tunnel closed.")
    }
}
