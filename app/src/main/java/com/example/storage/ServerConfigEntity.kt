package com.example.storage

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ServerConfig

@Entity(tableName = "server_configs")
data class ServerConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val protocol: String = "vless",
    val address: String,
    val port: Int,
    val uuid: String = "",
    val password: String = "",
    val method: String = "aes-256-gcm",
    val alterId: Int = 0,
    val encryption: String = "none",
    val transport: String = "tcp",
    val security: String = "tls",
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
    val ping: Long = 0,
    val uploadBytes: Long = 0,
    val downloadBytes: Long = 0,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ServerConfig = ServerConfig(
        id = id,
        name = name,
        protocol = protocol.ifBlank { "vless" },
        address = address,
        port = port,
        uuid = uuid,
        password = password,
        method = method.ifBlank { "aes-256-gcm" },
        alterId = alterId,
        encryption = encryption,
        transport = transport.ifBlank { "tcp" },
        security = security.ifBlank { "none" },
        sni = sni,
        alpn = alpn,
        fingerprint = fingerprint,
        publicKey = publicKey,
        shortId = shortId,
        spiderX = spiderX,
        wsPath = wsPath,
        wsHost = wsHost,
        flow = flow,
        network = network,
        groupName = groupName.ifBlank { "Default" },
        subUrl = subUrl,
        ping = ping,
        uploadBytes = uploadBytes,
        downloadBytes = downloadBytes,
        isActive = isActive,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(config: ServerConfig): ServerConfigEntity = ServerConfigEntity(
            id = config.id,
            name = config.name,
            protocol = config.protocol,
            address = config.address,
            port = config.port,
            uuid = config.uuid,
            password = config.password,
            method = config.method,
            alterId = config.alterId,
            encryption = config.encryption,
            transport = config.transport,
            security = config.security,
            sni = config.sni,
            alpn = config.alpn,
            fingerprint = config.fingerprint,
            publicKey = config.publicKey,
            shortId = config.shortId,
            spiderX = config.spiderX,
            wsPath = config.wsPath,
            wsHost = config.wsHost,
            flow = config.flow,
            network = config.network,
            groupName = config.groupName.ifBlank { "Default" },
            subUrl = config.subUrl,
            ping = config.ping,
            uploadBytes = config.uploadBytes,
            downloadBytes = config.downloadBytes,
            isActive = config.isActive,
            createdAt = config.createdAt
        )
    }
}
