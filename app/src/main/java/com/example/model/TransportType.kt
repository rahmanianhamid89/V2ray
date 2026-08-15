package com.example.model

enum class TransportType(val rawValue: String, val displayName: String) {
    TCP("tcp", "TCP"),
    WEBSOCKET("ws", "WebSocket");

    companion object {
        fun fromString(value: String?): TransportType {
            return when (value?.lowercase()) {
                "ws", "websocket" -> WEBSOCKET
                else -> TCP
            }
        }
    }
}
