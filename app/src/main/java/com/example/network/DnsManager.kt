package com.example.network

import com.example.storage.AppSettingsManager

object DnsManager {
    val defaultPrimaryDns = AppSettingsManager.DEFAULT_PRIMARY_DNS
    val defaultSecondaryDns = AppSettingsManager.DEFAULT_SECONDARY_DNS

    val currentPrimaryDns: String
        get() = AppSettingsManager.getPrimaryDns()

    val currentSecondaryDns: String
        get() = AppSettingsManager.getSecondaryDns()

    fun getDnsServers(): List<String> {
        return listOf(currentPrimaryDns, currentSecondaryDns)
    }

    fun setCustomDns(primary: String, secondary: String) {
        AppSettingsManager.setDns(primary, secondary)
    }
}
