package com.example.proxy

import com.example.model.ProxyProtocol
import com.example.model.ServerConfig
import org.json.JSONArray
import org.json.JSONObject

object XrayConfigJsonBuilder {

    fun buildXrayConfigJson(
        config: ServerConfig,
        localPort: Int = 10808,
        dnsServers: List<String> = listOf("1.1.1.1", "8.8.8.8")
    ): String {
        val root = JSONObject()

        // Log configuration
        val log = JSONObject().apply {
            put("loglevel", "warning")
        }
        root.put("log", log)

        // DNS configuration
        val dns = JSONObject().apply {
            val servers = JSONArray()
            dnsServers.forEach { servers.put(it) }
            servers.put("1.1.1.1")
            servers.put("8.8.8.8")
            put("servers", servers)
        }
        root.put("dns", dns)

        // Inbounds
        val inbounds = JSONArray()
        val socksInbound = JSONObject().apply {
            put("tag", "socks-in")
            put("port", localPort)
            put("listen", "127.0.0.1")
            put("protocol", "socks")
            put("settings", JSONObject().apply {
                put("auth", "noauth")
                put("udp", true)
            })
        }
        inbounds.put(socksInbound)
        root.put("inbounds", inbounds)

        // Outbounds
        val outbounds = JSONArray()
        val proxyOutbound = JSONObject().apply {
            put("tag", "proxy")

            when (config.proxyProtocol) {
                ProxyProtocol.VLESS -> {
                    put("protocol", "vless")
                    val vnext = JSONArray()
                    val serverObj = JSONObject().apply {
                        put("address", config.address)
                        put("port", config.port)
                        val users = JSONArray()
                        val userObj = JSONObject().apply {
                            put("id", config.uuid)
                            put("encryption", config.encryption.ifBlank { "none" })
                            if (config.flow.isNotBlank()) {
                                put("flow", config.flow)
                            }
                        }
                        users.put(userObj)
                        put("users", users)
                    }
                    vnext.put(serverObj)
                    put("settings", JSONObject().apply { put("vnext", vnext) })
                }

                ProxyProtocol.VMESS -> {
                    put("protocol", "vmess")
                    val vnext = JSONArray()
                    val serverObj = JSONObject().apply {
                        put("address", config.address)
                        put("port", config.port)
                        val users = JSONArray()
                        val userObj = JSONObject().apply {
                            put("id", config.uuid)
                            put("alterId", config.alterId)
                            put("security", config.encryption.ifBlank { "auto" })
                        }
                        users.put(userObj)
                        put("users", users)
                    }
                    vnext.put(serverObj)
                    put("settings", JSONObject().apply { put("vnext", vnext) })
                }

                ProxyProtocol.SHADOWSOCKS -> {
                    put("protocol", "shadowsocks")
                    val servers = JSONArray()
                    val serverObj = JSONObject().apply {
                        put("address", config.address)
                        put("port", config.port)
                        put("method", config.method.ifBlank { "aes-256-gcm" })
                        put("password", config.password)
                        put("ota", false)
                    }
                    servers.put(serverObj)
                    put("settings", JSONObject().apply { put("servers", servers) })
                }

                ProxyProtocol.TROJAN -> {
                    put("protocol", "trojan")
                    val servers = JSONArray()
                    val serverObj = JSONObject().apply {
                        put("address", config.address)
                        put("port", config.port)
                        put("password", config.password)
                    }
                    servers.put(serverObj)
                    put("settings", JSONObject().apply { put("servers", servers) })
                }
            }

            // Stream settings (for VLESS, VMess, Trojan, and WS Shadowsocks)
            val streamSettings = JSONObject().apply {
                put("network", config.transport.ifBlank { "tcp" })
                put("security", config.security.ifBlank { "none" })

                if (config.security == "tls") {
                    val tlsSettings = JSONObject().apply {
                        if (config.sni.isNotBlank()) put("serverName", config.sni)
                        if (config.fingerprint.isNotBlank()) put("fingerprint", config.fingerprint)
                        if (config.alpn.isNotBlank()) {
                            val alpnArray = JSONArray()
                            config.alpn.split(",").forEach { alpnArray.put(it.trim()) }
                            put("alpn", alpnArray)
                        }
                    }
                    put("tlsSettings", tlsSettings)
                } else if (config.security == "reality") {
                    val realitySettings = JSONObject().apply {
                        if (config.sni.isNotBlank()) put("serverName", config.sni)
                        if (config.fingerprint.isNotBlank()) put("fingerprint", config.fingerprint)
                        put("publicKey", config.publicKey)
                        put("shortId", config.shortId)
                        if (config.spiderX.isNotBlank()) put("spiderX", config.spiderX)
                    }
                    put("realitySettings", realitySettings)
                }

                if (config.transport == "ws") {
                    val wsSettings = JSONObject().apply {
                        put("path", config.wsPath.ifBlank { "/" })
                        if (config.wsHost.isNotBlank()) {
                            put("headers", JSONObject().apply {
                                put("Host", config.wsHost)
                            })
                        }
                    }
                    put("wsSettings", wsSettings)
                }
            }

            put("streamSettings", streamSettings)
        }
        outbounds.put(proxyOutbound)

        val directOutbound = JSONObject().apply {
            put("tag", "direct")
            put("protocol", "freedom")
        }
        outbounds.put(directOutbound)

        root.put("outbounds", outbounds)

        return root.toString(2)
    }
}
