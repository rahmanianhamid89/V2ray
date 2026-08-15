package com.example.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.logger.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppSettingsManager {
    private const val PREFS_NAME = "v2ray_app_settings"
    private const val KEY_PING_URL = "pref_ping_url"
    private const val KEY_PRIMARY_DNS = "pref_primary_dns"
    private const val KEY_SECONDARY_DNS = "pref_secondary_dns"
    private const val KEY_BYPASSED_APPS = "pref_bypassed_apps"

    const val DEFAULT_PING_URL = "https://www.gstatic.com/generate_204"
    const val DEFAULT_PRIMARY_DNS = "1.1.1.1"
    const val DEFAULT_SECONDARY_DNS = "8.8.8.8"

    private var prefs: SharedPreferences? = null

    private val _pingUrlFlow = MutableStateFlow(DEFAULT_PING_URL)
    val pingUrlFlow: StateFlow<String> = _pingUrlFlow.asStateFlow()

    private val _primaryDnsFlow = MutableStateFlow(DEFAULT_PRIMARY_DNS)
    val primaryDnsFlow: StateFlow<String> = _primaryDnsFlow.asStateFlow()

    private val _secondaryDnsFlow = MutableStateFlow(DEFAULT_SECONDARY_DNS)
    val secondaryDnsFlow: StateFlow<String> = _secondaryDnsFlow.asStateFlow()

    private val _bypassedAppsFlow = MutableStateFlow<Set<String>>(emptySet())
    val bypassedAppsFlow: StateFlow<Set<String>> = _bypassedAppsFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadValues()
        }
    }

    private fun loadValues() {
        val sp = prefs ?: return
        val ping = sp.getString(KEY_PING_URL, DEFAULT_PING_URL) ?: DEFAULT_PING_URL
        val pDns = sp.getString(KEY_PRIMARY_DNS, DEFAULT_PRIMARY_DNS) ?: DEFAULT_PRIMARY_DNS
        val sDns = sp.getString(KEY_SECONDARY_DNS, DEFAULT_SECONDARY_DNS) ?: DEFAULT_SECONDARY_DNS
        val bypassed = sp.getStringSet(KEY_BYPASSED_APPS, emptySet()) ?: emptySet()

        _pingUrlFlow.value = ping
        _primaryDnsFlow.value = pDns
        _secondaryDnsFlow.value = sDns
        _bypassedAppsFlow.value = bypassed
    }

    fun getPingUrl(): String = _pingUrlFlow.value

    fun setPingUrl(url: String) {
        val trimmed = url.trim().ifBlank { DEFAULT_PING_URL }
        _pingUrlFlow.value = trimmed
        prefs?.edit()?.putString(KEY_PING_URL, trimmed)?.apply()
        AppLogger.i("AppSettingsManager", "Saved Ping Test URL: $trimmed")
    }

    fun getPrimaryDns(): String = _primaryDnsFlow.value
    fun getSecondaryDns(): String = _secondaryDnsFlow.value

    fun setDns(primary: String, secondary: String) {
        val p = primary.trim().ifBlank { DEFAULT_PRIMARY_DNS }
        val s = secondary.trim().ifBlank { DEFAULT_SECONDARY_DNS }
        _primaryDnsFlow.value = p
        _secondaryDnsFlow.value = s
        prefs?.edit()
            ?.putString(KEY_PRIMARY_DNS, p)
            ?.putString(KEY_SECONDARY_DNS, s)
            ?.apply()
        AppLogger.i("AppSettingsManager", "Saved Custom DNS: $p, $s")
    }

    fun getBypassedApps(): Set<String> = _bypassedAppsFlow.value

    fun setBypassedApps(packages: Set<String>) {
        _bypassedAppsFlow.value = packages
        prefs?.edit()?.putStringSet(KEY_BYPASSED_APPS, packages)?.apply()
        AppLogger.i("AppSettingsManager", "Updated bypassed apps list (${packages.size} apps)")
    }

    fun toggleAppBypass(packageName: String) {
        val current = _bypassedAppsFlow.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        setBypassedApps(current)
    }
}
