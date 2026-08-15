package com.example.proxy

import com.example.model.ServerConfig

object VlessUriParser {

    fun parse(uriString: String): Result<ServerConfig> {
        return UniversalConfigParser.parse(uriString)
    }

    fun toVlessUrl(config: ServerConfig): String {
        return UniversalConfigParser.toShareUrl(config)
    }
}
