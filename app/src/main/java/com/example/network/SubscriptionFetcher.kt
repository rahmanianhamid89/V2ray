package com.example.network

import android.util.Base64
import com.example.logger.AppLogger
import com.example.model.ServerConfig
import com.example.proxy.UniversalConfigParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object SubscriptionFetcher {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchSubscription(
        subUrl: String,
        targetGroupName: String
    ): Result<List<ServerConfig>> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = subUrl.trim()
            if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
                !cleanUrl.startsWith("https://", ignoreCase = true)
            ) {
                return@withContext Result.failure(IllegalArgumentException("Subscription URL must start with http:// or https://"))
            }

            AppLogger.i("SubscriptionFetcher", "Fetching subscription from: $cleanUrl")

            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", "v2rayN/6.23 clash-verge/1.3.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val body = response.body?.string() ?: ""
            if (body.isBlank()) {
                return@withContext Result.failure(Exception("Subscription response body was empty."))
            }

            // Attempt to decode base64 or treat as raw multi-line config
            val decodedContent = decodeSubscriptionContent(body)

            val lines = decodedContent.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val parsedConfigs = mutableListOf<ServerConfig>()

            for (line in lines) {
                val parseRes = UniversalConfigParser.parse(line)
                parseRes.onSuccess { cfg ->
                    parsedConfigs.add(
                        cfg.copy(
                            groupName = targetGroupName.ifBlank { "Sub-Import" },
                            subUrl = cleanUrl
                        )
                    )
                }
            }

            if (parsedConfigs.isEmpty()) {
                return@withContext Result.failure(
                    Exception("No valid proxy configurations (VLESS, VMess, Shadowsocks, Trojan) found in subscription content (${lines.size} lines received).")
                )
            }

            AppLogger.i("SubscriptionFetcher", "Successfully parsed ${parsedConfigs.size} configs (VLESS/VMess/SS/Trojan) into group '$targetGroupName'")
            Result.success(parsedConfigs)
        } catch (e: Exception) {
            AppLogger.e("SubscriptionFetcher", "Failed to fetch subscription: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    private fun decodeSubscriptionContent(raw: String): String {
        val trimmed = raw.trim().replace("\r\n", "\n").replace("\r", "\n")

        // If content already contains explicit schemes on multiple lines, return raw
        if (trimmed.contains("vless://") || trimmed.contains("vmess://") || trimmed.contains("trojan://") || trimmed.contains("ss://")) {
            return trimmed
        }

        // Try standard base64 decoding
        try {
            val compact = trimmed.replace("\n", "").replace(" ", "")
            val decodedBytes = Base64.decode(compact, Base64.DEFAULT or Base64.NO_WRAP or Base64.URL_SAFE)
            val decodedStr = String(decodedBytes, StandardCharsets.UTF_8)
            if (decodedStr.contains("://") || decodedStr.lines().size > 1) {
                return decodedStr
            }
        } catch (e: Exception) {
            // Ignore and fall back to raw
        }

        return trimmed
    }
}
