package com.example.model

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connecting(val stage: String) : ConnectionState()
    data class Connected(
        val serverAddress: String,
        val protocol: String = "VLESS",
        val latencyMs: Long = 0,
        val publicIp: String? = null,
        val connectedSince: Long = System.currentTimeMillis()
    ) : ConnectionState()
    object Disconnecting : ConnectionState()
    data class Error(val message: String, val detail: String? = null) : ConnectionState()
}
