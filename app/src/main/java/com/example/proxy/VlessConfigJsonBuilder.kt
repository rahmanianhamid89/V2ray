package com.example.proxy

import com.example.model.ServerConfig

object VlessConfigJsonBuilder {

    fun buildXrayConfigJson(
        config: ServerConfig,
        localPort: Int = 10808,
        dnsServers: List<String> = listOf("1.1.1.1", "8.8.8.8")
    ): String {
        return XrayConfigJsonBuilder.buildXrayConfigJson(config, localPort, dnsServers)
    }
}
