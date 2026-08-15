package com.example.network

import com.example.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ConnectivityTester {

    const val DEFAULT_PING_URL = "https://www.gstatic.com/generate_204"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    data class TestResult(
        val isSuccessful: Boolean,
        val latencyMs: Long,
        val publicIp: String?,
        val errorMessage: String? = null
    )

    suspend fun testConnection(pingUrl: String = DEFAULT_PING_URL): TestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            // First: Test HTTP connectivity & latency to requested default 204 endpoint
            val pingRequest = Request.Builder()
                .url(pingUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; V2RayVPN/1.0)")
                .build()

            val pingResponse = client.newCall(pingRequest).execute()
            val endTime = System.currentTimeMillis()
            val latency = (endTime - startTime).coerceAtLeast(1)

            if (!pingResponse.isSuccessful && pingResponse.code != 204) {
                AppLogger.w("ConnectivityTester", "Ping returned HTTP code ${pingResponse.code}")
            }

            // Second: Fetch external public IP address
            var publicIp: String? = null
            try {
                val ipRequest = Request.Builder()
                    .url("https://api.ipify.org?format=json")
                    .build()

                val ipResponse = client.newCall(ipRequest).execute()
                if (ipResponse.isSuccessful) {
                    val bodyStr = ipResponse.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    publicIp = json.optString("ip", null)
                }
            } catch (e: Exception) {
                AppLogger.w("ConnectivityTester", "Public IP fetch failed: ${e.localizedMessage}")
            }

            AppLogger.i("ConnectivityTester", "Connectivity test passed ($pingUrl). Latency: ${latency}ms, IP: $publicIp")
            TestResult(
                isSuccessful = true,
                latencyMs = latency,
                publicIp = publicIp
            )
        } catch (e: Exception) {
            AppLogger.e("ConnectivityTester", "Connectivity test failed: ${e.localizedMessage}", e)
            TestResult(
                isSuccessful = false,
                latencyMs = 0,
                publicIp = null,
                errorMessage = e.localizedMessage ?: "Network request timed out"
            )
        }
    }
}
