package com.example.model

enum class SecurityType(val rawValue: String, val displayName: String) {
    NONE("none", "None"),
    TLS("tls", "TLS"),
    REALITY("reality", "REALITY");

    companion object {
        fun fromString(value: String?): SecurityType {
            return when (value?.lowercase()) {
                "tls" -> TLS
                "reality" -> REALITY
                else -> NONE
            }
        }
    }
}
