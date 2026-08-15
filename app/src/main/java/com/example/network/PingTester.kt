package com.example.network

import com.example.logger.AppLogger
import com.example.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object PingTester {

    suspend fun testServerTcpPing(address: String, port: Int, timeoutMs: Int = 3500): Long = withContext(Dispatchers.IO) {
        if (address.isBlank() || port <= 0) return@withContext -1L
        var socket: Socket? = null
        val start = System.currentTimeMillis()
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            val socketAddress = InetSocketAddress(address, port)
            socket.connect(socketAddress, timeoutMs)
            val latency = (System.currentTimeMillis() - start).coerceAtLeast(1)
            AppLogger.d("PingTester", "Ping to $address:$port -> ${latency}ms")
            latency
        } catch (e: Exception) {
            AppLogger.d("PingTester", "Ping failed for $address:$port -> ${e.message}")
            -1L
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    suspend fun batchTestPings(
        configs: List<ServerConfig>,
        onConfigTested: suspend (ServerConfig, Long) -> Unit
    ) = coroutineScope {
        val semaphore = Semaphore(8)
        val jobs = configs.map { config ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val ping = testServerTcpPing(config.address, config.port)
                    onConfigTested(config, ping)
                    ping
                }
            }
        }
        jobs.awaitAll()
    }

    /**
     * Default sort: lowest ping first (کمترین پینگ اول)
     * 1) Ping > 0 sorted ascending (30ms < 80ms < 200ms)
     * 2) Untested (ping == 0)
     * 3) Timeout / Error (ping == -1)
     */
    fun sortByLowestPing(configs: List<ServerConfig>): List<ServerConfig> {
        return configs.sortedWith { a, b ->
            val rankA = getPingSortRank(a.ping)
            val rankB = getPingSortRank(b.ping)
            if (rankA != rankB) {
                rankA.compareTo(rankB)
            } else if (a.ping > 0 && b.ping > 0) {
                a.ping.compareTo(b.ping)
            } else {
                b.createdAt.compareTo(a.createdAt)
            }
        }
    }

    private fun getPingSortRank(ping: Long): Int {
        return when {
            ping > 0 -> 1 // Lowest valid ping first
            ping == 0L -> 2 // Untested in middle
            else -> 3 // Timeout / failed at the end
        }
    }
}
