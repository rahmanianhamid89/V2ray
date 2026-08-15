package com.example.proxy

import android.net.Uri
import android.util.Base64
import com.example.logger.AppLogger
import com.example.model.ProxyProtocol
import com.example.model.ServerConfig
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object UniversalConfigParser {

    fun parse(rawInput: String): Result<ServerConfig> {
        val trimmed = rawInput.trim()
        return when {
            trimmed.startsWith("{") -> parseJson(trimmed)
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) || trimmed.startsWith("shadowsocks://", ignoreCase = true) -> parseShadowsocks(trimmed)
            else -> Result.failure(IllegalArgumentException("Unsupported protocol scheme. Expected vless://, vmess://, ss://, trojan:// or JSON"))
        }
    }

    fun parseJson(jsonString: String): Result<ServerConfig> {
        return try {
            val root = JSONObject(jsonString.trim())
            // Check if full xray config with outbounds or single outbound
            val outbound = if (root.has("outbounds")) {
                val outbounds = root.getJSONArray("outbounds")
                if (outbounds.length() > 0) outbounds.getJSONObject(0) else root
            } else if (root.has("protocol")) {
                root
            } else {
                root
            }

            val proto = outbound.optString("protocol", "vless").lowercase()
            val tag = outbound.optString("tag", "JSON Imported Config")
            val settings = outbound.optJSONObject("settings") ?: JSONObject()
            val streamSettings = outbound.optJSONObject("streamSettings") ?: JSONObject()

            var address = "127.0.0.1"
            var port = 443
            var uuid = ""
            var password = ""
            var method = "aes-256-gcm"
            var alterId = 0
            var flow = ""

            when (proto) {
                "vless" -> {
                    val vnext = settings.optJSONArray("vnext")
                    if (vnext != null && vnext.length() > 0) {
                        val firstVnext = vnext.getJSONObject(0)
                        address = firstVnext.optString("address", "127.0.0.1")
                        port = firstVnext.optInt("port", 443)
                        val users = firstVnext.optJSONArray("users")
                        if (users != null && users.length() > 0) {
                            val user = users.getJSONObject(0)
                            uuid = user.optString("id", "")
                            flow = user.optString("flow", "")
                        }
                    }
                }
                "vmess" -> {
                    val vnext = settings.optJSONArray("vnext")
                    if (vnext != null && vnext.length() > 0) {
                        val firstVnext = vnext.getJSONObject(0)
                        address = firstVnext.optString("address", "127.0.0.1")
                        port = firstVnext.optInt("port", 443)
                        val users = firstVnext.optJSONArray("users")
                        if (users != null && users.length() > 0) {
                            val user = users.getJSONObject(0)
                            uuid = user.optString("id", "")
                            alterId = user.optInt("alterId", 0)
                        }
                    }
                }
                "trojan" -> {
                    val servers = settings.optJSONArray("servers")
                    if (servers != null && servers.length() > 0) {
                        val firstServer = servers.getJSONObject(0)
                        address = firstServer.optString("address", "127.0.0.1")
                        port = firstServer.optInt("port", 443)
                        password = firstServer.optString("password", "")
                    }
                }
                "shadowsocks", "ss" -> {
                    val servers = settings.optJSONArray("servers")
                    if (servers != null && servers.length() > 0) {
                        val firstServer = servers.getJSONObject(0)
                        address = firstServer.optString("address", "127.0.0.1")
                        port = firstServer.optInt("port", 8388)
                        password = firstServer.optString("password", "")
                        method = firstServer.optString("method", "aes-256-gcm")
                    }
                }
            }

            val transport = streamSettings.optString("network", "tcp")
            val security = streamSettings.optString("security", if (proto == "shadowsocks") "none" else "tls")
            val tlsSettings = streamSettings.optJSONObject("tlsSettings") ?: streamSettings.optJSONObject("realitySettings") ?: JSONObject()
            val sni = tlsSettings.optString("serverName", "")
            val fp = tlsSettings.optString("fingerprint", "chrome")
            val wsSettings = streamSettings.optJSONObject("wsSettings") ?: JSONObject()
            val wsPath = wsSettings.optString("path", "/")
            val wsHeaders = wsSettings.optJSONObject("headers")
            val wsHost = wsHeaders?.optString("Host", "") ?: ""

            val config = ServerConfig(
                name = tag.ifBlank { "$address:$port" },
                protocol = proto,
                address = address,
                port = port,
                uuid = uuid,
                password = password,
                method = method,
                alterId = alterId,
                transport = transport,
                security = security,
                sni = sni,
                fingerprint = fp,
                wsPath = wsPath,
                wsHost = wsHost,
                flow = flow,
                groupName = "Default"
            )
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseVless(uriString: String): Result<ServerConfig> {
        return try {
            val trimmed = uriString.trim()
            val uri = Uri.parse(trimmed)
            val userInfo = uri.userInfo ?: ""
            val uuid = userInfo.ifBlank {
                val rawNoScheme = trimmed.substringAfter("://")
                val atIdx = rawNoScheme.indexOf('@')
                if (atIdx > 0) rawNoScheme.substring(0, atIdx) else ""
            }

            if (uuid.isBlank()) {
                return Result.failure(IllegalArgumentException("Missing UUID in VLESS URL"))
            }

            val host = uri.host ?: ""
            val port = if (uri.port != -1) uri.port else 443
            if (host.isBlank()) {
                return Result.failure(IllegalArgumentException("Missing server host in VLESS URL"))
            }

            val name = if (!uri.fragment.isNullOrBlank()) {
                try { URLDecoder.decode(uri.fragment, StandardCharsets.UTF_8.name()) } catch (e: Exception) { uri.fragment ?: "$host:$port" }
            } else {
                "$host:$port"
            }

            val transport = uri.getQueryParameter("type") ?: uri.getQueryParameter("net") ?: "tcp"
            val security = uri.getQueryParameter("security") ?: "none"
            val sni = uri.getQueryParameter("sni") ?: ""
            val alpn = uri.getQueryParameter("alpn") ?: ""
            val fp = uri.getQueryParameter("fp") ?: "chrome"
            val pbk = uri.getQueryParameter("pbk") ?: ""
            val sid = uri.getQueryParameter("sid") ?: ""
            val spx = uri.getQueryParameter("spx") ?: ""
            val rawPath = uri.getQueryParameter("path") ?: "/"
            val path = try { URLDecoder.decode(rawPath, StandardCharsets.UTF_8.name()) } catch (e: Exception) { rawPath }
            val wsHost = uri.getQueryParameter("host") ?: ""
            val flow = uri.getQueryParameter("flow") ?: ""
            val encryption = uri.getQueryParameter("encryption") ?: "none"

            val config = ServerConfig(
                protocol = "vless",
                name = name,
                address = host,
                port = port,
                uuid = uuid,
                encryption = encryption,
                transport = transport,
                security = security,
                sni = sni,
                alpn = alpn,
                fingerprint = fp,
                publicKey = pbk,
                shortId = sid,
                spiderX = spx,
                wsPath = path,
                wsHost = wsHost,
                flow = flow,
                network = transport
            )

            AppLogger.i("UniversalConfigParser", "Parsed VLESS: $name ($host:$port)")
            Result.success(config)
        } catch (e: Exception) {
            AppLogger.e("UniversalConfigParser", "Failed to parse VLESS link", e)
            Result.failure(e)
        }
    }

    fun parseVmess(rawInput: String): Result<ServerConfig> {
        return try {
            val trimmed = rawInput.trim()
            val rawPayload = trimmed.substringAfter("://")

            // Check if it's base64 JSON (standard V2RayN format)
            val decodedString = safeBase64Decode(rawPayload)
            if (decodedString.startsWith("{") && decodedString.endsWith("}")) {
                val json = JSONObject(decodedString)
                val ps = json.optString("ps", "VMess Server")
                val add = json.optString("add", "")
                val port = json.optInt("port", 443)
                val id = json.optString("id", "")
                val aid = json.optInt("aid", 0)
                val scy = json.optString("scy", "auto")
                val net = json.optString("net", "tcp")
                val type = json.optString("type", "none")
                val host = json.optString("host", "")
                val path = json.optString("path", "/")
                val tls = json.optString("tls", "none")
                val sni = json.optString("sni", host)
                val alpn = json.optString("alpn", "")
                val fp = json.optString("fp", "chrome")

                if (add.isBlank() || id.isBlank()) {
                    return Result.failure(IllegalArgumentException("VMess JSON missing address or UUID"))
                }

                val config = ServerConfig(
                    protocol = "vmess",
                    name = ps.ifBlank { "$add:$port" },
                    address = add,
                    port = port,
                    uuid = id,
                    alterId = aid,
                    security = if (tls.equals("tls", ignoreCase = true)) "tls" else "none",
                    encryption = scy,
                    transport = net.ifBlank { "tcp" },
                    wsPath = path.ifBlank { "/" },
                    wsHost = host,
                    sni = sni,
                    alpn = alpn,
                    fingerprint = fp
                )
                AppLogger.i("UniversalConfigParser", "Parsed VMess: ${config.name} ($add:$port)")
                return Result.success(config)
            }

            // Fallback standard URI scheme format vmess://uuid@host:port?params#name
            val uri = Uri.parse(trimmed)
            val uuid = uri.userInfo ?: ""
            val host = uri.host ?: ""
            val port = if (uri.port != -1) uri.port else 443
            val name = if (!uri.fragment.isNullOrBlank()) {
                try { URLDecoder.decode(uri.fragment, StandardCharsets.UTF_8.name()) } catch (e: Exception) { uri.fragment ?: "$host:$port" }
            } else {
                "$host:$port"
            }

            val transport = uri.getQueryParameter("type") ?: uri.getQueryParameter("net") ?: "tcp"
            val security = uri.getQueryParameter("security") ?: "none"
            val sni = uri.getQueryParameter("sni") ?: ""
            val rawPath = uri.getQueryParameter("path") ?: "/"
            val path = try { URLDecoder.decode(rawPath, StandardCharsets.UTF_8.name()) } catch (e: Exception) { rawPath }
            val wsHost = uri.getQueryParameter("host") ?: ""

            val config = ServerConfig(
                protocol = "vmess",
                name = name,
                address = host,
                port = port,
                uuid = uuid,
                transport = transport,
                security = security,
                sni = sni,
                wsPath = path,
                wsHost = wsHost
            )
            Result.success(config)
        } catch (e: Exception) {
            AppLogger.e("UniversalConfigParser", "Failed to parse VMess link", e)
            Result.failure(e)
        }
    }

    fun parseTrojan(rawInput: String): Result<ServerConfig> {
        return try {
            val trimmed = rawInput.trim()
            val uri = Uri.parse(trimmed)
            val password = uri.userInfo ?: ""
            val host = uri.host ?: ""
            val port = if (uri.port != -1) uri.port else 443

            if (host.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Missing host or password in Trojan URL"))
            }

            val name = if (!uri.fragment.isNullOrBlank()) {
                try { URLDecoder.decode(uri.fragment, StandardCharsets.UTF_8.name()) } catch (e: Exception) { uri.fragment ?: "$host:$port" }
            } else {
                "$host:$port"
            }

            val transport = uri.getQueryParameter("type") ?: "tcp"
            val security = uri.getQueryParameter("security") ?: "tls"
            val sni = uri.getQueryParameter("sni") ?: host
            val alpn = uri.getQueryParameter("alpn") ?: ""
            val fp = uri.getQueryParameter("fp") ?: "chrome"
            val rawPath = uri.getQueryParameter("path") ?: "/"
            val path = try { URLDecoder.decode(rawPath, StandardCharsets.UTF_8.name()) } catch (e: Exception) { rawPath }
            val wsHost = uri.getQueryParameter("host") ?: ""

            val config = ServerConfig(
                protocol = "trojan",
                name = name,
                address = host,
                port = port,
                password = password,
                transport = transport,
                security = security,
                sni = sni,
                alpn = alpn,
                fingerprint = fp,
                wsPath = path,
                wsHost = wsHost
            )

            AppLogger.i("UniversalConfigParser", "Parsed Trojan: $name ($host:$port)")
            Result.success(config)
        } catch (e: Exception) {
            AppLogger.e("UniversalConfigParser", "Failed to parse Trojan link", e)
            Result.failure(e)
        }
    }

    fun parseShadowsocks(rawInput: String): Result<ServerConfig> {
        return try {
            val trimmed = rawInput.trim()
            val rawNoScheme = trimmed.substringAfter("://")
            val hashIdx = rawNoScheme.indexOf('#')
            val name = if (hashIdx > 0 && hashIdx < rawNoScheme.length - 1) {
                try { URLDecoder.decode(rawNoScheme.substring(hashIdx + 1), StandardCharsets.UTF_8.name()) } catch (e: Exception) { rawNoScheme.substring(hashIdx + 1) }
            } else {
                "Shadowsocks Node"
            }

            val body = if (hashIdx > 0) rawNoScheme.substring(0, hashIdx) else rawNoScheme
            var method = "aes-256-gcm"
            var password = ""
            var host = ""
            var port = 8388

            if (body.contains("@")) {
                // SIP002 format: ss://BASE64(method:password)@host:port/?plugin=...
                val atIdx = body.indexOf('@')
                val userPart = body.substring(0, atIdx)
                val serverPart = body.substring(atIdx + 1)

                val decodedUser = safeBase64Decode(userPart)
                if (decodedUser.contains(":")) {
                    method = decodedUser.substringBefore(":")
                    password = decodedUser.substringAfter(":")
                }

                val cleanServer = serverPart.substringBefore("/").substringBefore("?")
                if (cleanServer.contains(":")) {
                    host = cleanServer.substringBefore(":")
                    port = cleanServer.substringAfter(":").toIntOrNull() ?: 8388
                } else {
                    host = cleanServer
                }
            } else {
                // Legacy format: ss://BASE64(method:password@host:port)
                val decodedAll = safeBase64Decode(body)
                if (decodedAll.contains("@")) {
                    val userPart = decodedAll.substringBefore("@")
                    val serverPart = decodedAll.substringAfter("@")
                    if (userPart.contains(":")) {
                        method = userPart.substringBefore(":")
                        password = userPart.substringAfter(":")
                    }
                    val cleanServer = serverPart.substringBefore("/").substringBefore("?")
                    if (cleanServer.contains(":")) {
                        host = cleanServer.substringBefore(":")
                        port = cleanServer.substringAfter(":").toIntOrNull() ?: 8388
                    } else {
                        host = cleanServer
                    }
                }
            }

            if (host.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Missing host or password in Shadowsocks link"))
            }

            val config = ServerConfig(
                protocol = "shadowsocks",
                name = if (name != "Shadowsocks Node") name else "$host:$port",
                address = host,
                port = port,
                password = password,
                method = method,
                transport = "tcp",
                security = "none"
            )

            AppLogger.i("UniversalConfigParser", "Parsed Shadowsocks: ${config.name} ($host:$port, $method)")
            Result.success(config)
        } catch (e: Exception) {
            AppLogger.e("UniversalConfigParser", "Failed to parse Shadowsocks link", e)
            Result.failure(e)
        }
    }

    fun toShareUrl(config: ServerConfig): String {
        return try {
            when (config.proxyProtocol) {
                ProxyProtocol.VLESS -> toVlessUrl(config)
                ProxyProtocol.VMESS -> toVmessUrl(config)
                ProxyProtocol.TROJAN -> toTrojanUrl(config)
                ProxyProtocol.SHADOWSOCKS -> toShadowsocksUrl(config)
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun toVlessUrl(config: ServerConfig): String {
        val encPath = URLEncoder.encode(config.wsPath, StandardCharsets.UTF_8.name())
        val encName = URLEncoder.encode(config.name, StandardCharsets.UTF_8.name())

        val sb = StringBuilder("vless://")
        sb.append(config.uuid).append("@").append(config.address).append(":").append(config.port)
        sb.append("?type=").append(config.transport)
        sb.append("&security=").append(config.security)
        if (config.sni.isNotBlank()) sb.append("&sni=").append(config.sni)
        if (config.alpn.isNotBlank()) sb.append("&alpn=").append(config.alpn)
        if (config.fingerprint.isNotBlank()) sb.append("&fp=").append(config.fingerprint)
        if (config.publicKey.isNotBlank()) sb.append("&pbk=").append(config.publicKey)
        if (config.shortId.isNotBlank()) sb.append("&sid=").append(config.shortId)
        if (config.spiderX.isNotBlank()) sb.append("&spx=").append(config.spiderX)
        if (config.wsPath.isNotBlank()) sb.append("&path=").append(encPath)
        if (config.wsHost.isNotBlank()) sb.append("&host=").append(config.wsHost)
        if (config.flow.isNotBlank()) sb.append("&flow=").append(config.flow)
        if (config.encryption.isNotBlank()) sb.append("&encryption=").append(config.encryption)
        sb.append("#").append(encName)
        return sb.toString()
    }

    fun toVmessUrl(config: ServerConfig): String {
        val json = JSONObject().apply {
            put("v", "2")
            put("ps", config.name)
            put("add", config.address)
            put("port", config.port)
            put("id", config.uuid)
            put("aid", config.alterId)
            put("scy", config.encryption.ifBlank { "auto" })
            put("net", config.transport)
            put("type", "none")
            put("host", config.wsHost)
            put("path", config.wsPath)
            put("tls", if (config.security == "tls") "tls" else "none")
            put("sni", config.sni)
            put("alpn", config.alpn)
            put("fp", config.fingerprint)
        }
        val encoded = Base64.encodeToString(json.toString().toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return "vmess://$encoded"
    }

    fun toTrojanUrl(config: ServerConfig): String {
        val encPath = URLEncoder.encode(config.wsPath, StandardCharsets.UTF_8.name())
        val encName = URLEncoder.encode(config.name, StandardCharsets.UTF_8.name())

        val sb = StringBuilder("trojan://")
        sb.append(config.password).append("@").append(config.address).append(":").append(config.port)
        sb.append("?security=").append(config.security.ifBlank { "tls" })
        sb.append("&type=").append(config.transport)
        if (config.sni.isNotBlank()) sb.append("&sni=").append(config.sni)
        if (config.alpn.isNotBlank()) sb.append("&alpn=").append(config.alpn)
        if (config.fingerprint.isNotBlank()) sb.append("&fp=").append(config.fingerprint)
        if (config.wsPath.isNotBlank()) sb.append("&path=").append(encPath)
        if (config.wsHost.isNotBlank()) sb.append("&host=").append(config.wsHost)
        sb.append("#").append(encName)
        return sb.toString()
    }

    fun toShadowsocksUrl(config: ServerConfig): String {
        val encName = URLEncoder.encode(config.name, StandardCharsets.UTF_8.name())
        val userString = "${config.method}:${config.password}"
        val userBase64 = Base64.encodeToString(userString.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return "ss://$userBase64@${config.address}:${config.port}#$encName"
    }

    private fun safeBase64Decode(str: String): String {
        return try {
            val clean = str.trim().replace("\n", "").replace("\r", "").replace(" ", "")
            val decoded = Base64.decode(clean, Base64.DEFAULT or Base64.NO_WRAP or Base64.URL_SAFE)
            String(decoded, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            str
        }
    }
}
