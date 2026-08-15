package com.example.model

data class ServerConfig(
    val id: Long = 0,
    val name: String = "Server Node",
    val protocol: String = "vless", // "vless", "vmess", "shadowsocks", "trojan"
    val address: String = "",
    val port: Int = 443,
    val uuid: String = "",           // Used for VLESS / VMess
    val password: String = "",       // Used for Trojan / Shadowsocks
    val method: String = "aes-256-gcm", // Used for Shadowsocks cipher
    val alterId: Int = 0,            // Used for VMess alterId
    val encryption: String = "none",
    val transport: String = "tcp",   // "tcp", "ws", "grpc", "http"
    val security: String = "tls",    // "none", "tls", "reality", "auto"
    val sni: String = "",
    val alpn: String = "",
    val fingerprint: String = "chrome",
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    val wsPath: String = "/",
    val wsHost: String = "",
    val flow: String = "",
    val network: String = "tcp",
    val groupName: String = "Default",
    val subUrl: String = "",
    val ping: Long = 0, // 0 = untested, >0 = ms, -1 = timeout/error
    val uploadBytes: Long = 0,
    val downloadBytes: Long = 0,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalTrafficBytes: Long
        get() = uploadBytes + downloadBytes

    val trafficDisplayText: String
        get() = formatTraffic(totalTrafficBytes)

    fun formatTraffic(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    val proxyProtocol: ProxyProtocol
        get() = ProxyProtocol.fromString(protocol)

    val transportType: TransportType
        get() = TransportType.fromString(transport)

    val securityType: SecurityType
        get() = SecurityType.fromString(security)

    fun getDisplayTitle(): String {
        return name.ifBlank { "$address:$port" }
    }

    val protocolBadgeText: String
        get() = when (proxyProtocol) {
            ProxyProtocol.VLESS -> "VLESS"
            ProxyProtocol.VMESS -> "VMESS"
            ProxyProtocol.SHADOWSOCKS -> "SS"
            ProxyProtocol.TROJAN -> "TROJAN"
        }

    val pingDisplayText: String
        get() = when {
            ping > 0 -> "${ping} ms"
            ping == -1L -> "Timeout"
            else -> "—"
        }
}
