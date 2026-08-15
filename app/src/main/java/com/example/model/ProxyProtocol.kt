package com.example.model

enum class ProxyProtocol(val rawValue: String, val displayName: String) {
    VLESS("vless", "VLESS"),
    VMESS("vmess", "VMess"),
    SHADOWSOCKS("shadowsocks", "Shadowsocks"),
    TROJAN("trojan", "Trojan");

    companion object {
        fun fromString(value: String?): ProxyProtocol {
            return when (value?.lowercase()) {
                "vmess" -> VMESS
                "shadowsocks", "ss" -> SHADOWSOCKS
                "trojan" -> TROJAN
                else -> VLESS
            }
        }
    }
}
