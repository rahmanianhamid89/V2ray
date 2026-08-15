package com.example.ui

import android.app.Application
import android.content.Context
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.logger.AppLogger
import com.example.model.ConnectionState
import com.example.model.LogEntry
import com.example.model.ServerConfig
import com.example.model.VpnStats
import com.example.network.ConnectivityTester
import com.example.network.PingTester
import com.example.network.SubscriptionFetcher
import com.example.proxy.UniversalConfigParser
import com.example.proxy.VlessUriParser
import com.example.storage.AppSettingsManager
import com.example.storage.ConfigRepository
import com.example.vpn.VpnController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConfigRepository.getInstance(application)

    init {
        AppSettingsManager.init(application)
    }

    // Raw configs from repository
    val allConfigs: StateFlow<List<ServerConfig>> = repository.allConfigs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sortOption = MutableStateFlow(com.example.model.ConfigSortOption.PING)
    val sortOption: StateFlow<com.example.model.ConfigSortOption> = _sortOption.asStateFlow()

    private val _customGroups = MutableStateFlow<Set<String>>(emptySet())

    val groups: StateFlow<List<String>> = repository.distinctGroups
        .map { dbGroups ->
            val merged = (dbGroups + _customGroups.value).filter { it.isNotBlank() }.distinct().toMutableList()
            if (!merged.contains("Default")) {
                merged.add(0, "Default")
            }
            merged
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("Default")
        )

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()

    val activeConfig: StateFlow<ServerConfig?> = repository.activeConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val connectionState: StateFlow<ConnectionState> = VpnController.connectionState
    val vpnStats: StateFlow<VpnStats> = VpnController.vpnStats
    val logs: StateFlow<List<LogEntry>> = AppLogger.logsFlow

    private val _pingResult = MutableStateFlow<String?>(null)
    val pingResult: StateFlow<String?> = _pingResult.asStateFlow()

    private val _isTestingPing = MutableStateFlow(false)
    val isTestingPing: StateFlow<Boolean> = _isTestingPing.asStateFlow()

    private val _isGroupPingTesting = MutableStateFlow(false)
    val isGroupPingTesting: StateFlow<Boolean> = _isGroupPingTesting.asStateFlow()

    private val _groupPingProgress = MutableStateFlow(0f)
    val groupPingProgress: StateFlow<Float> = _groupPingProgress.asStateFlow()

    init {
        viewModelScope.launch {
            repository.deleteAllConfigs()
        }
    }

    fun setSelectedGroup(group: String) {
        _selectedGroup.value = group
    }

    fun setSortOption(option: com.example.model.ConfigSortOption) {
        _sortOption.value = option
    }

    fun resetTraffic(config: ServerConfig) {
        viewModelScope.launch {
            repository.resetTraffic(config.id)
            AppLogger.i("MainViewModel", "Reset traffic for config: ${config.getDisplayTitle()}")
        }
    }

    fun resetAllTraffic() {
        viewModelScope.launch {
            repository.resetAllTraffic()
            AppLogger.i("MainViewModel", "Reset traffic for all configs.")
        }
    }

    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            _customGroups.value = _customGroups.value + trimmed
            _selectedGroup.value = trimmed
            AppLogger.i("MainViewModel", "Created custom group: $trimmed")
        }
    }

    fun deleteGroup(groupName: String) {
        viewModelScope.launch {
            repository.deleteConfigsByGroup(groupName)
            _customGroups.value = _customGroups.value - groupName
            _selectedGroup.value = "All"
            AppLogger.i("MainViewModel", "Deleted group: $groupName")
        }
    }

    fun prepareVpnAndConnect(context: Context, onPermissionNeeded: () -> Unit) {
        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            AppLogger.i("MainViewModel", "VPN Permission required. Triggering permission dialog.")
            onPermissionNeeded()
        } else {
            val server = activeConfig.value
            if (server != null) {
                AppLogger.i("MainViewModel", "VPN Permission granted. Starting VPN (${server.protocol.uppercase()})...")
                VpnController.startVpn(context, server)
            } else {
                AppLogger.w("MainViewModel", "Cannot start VPN: No active server config.")
                VpnController.updateConnectionState(
                    ConnectionState.Error("No Server Selected", "Please select or import a configuration first.")
                )
            }
        }
    }

    fun startVpnDirect(context: Context) {
        val server = activeConfig.value
        if (server != null) {
            VpnController.startVpn(context, server)
        }
    }

    fun stopVpn(context: Context) {
        VpnController.stopVpn(context)
    }

    fun importConfig(configUrl: String, targetGroup: String = "Default"): Result<ServerConfig> {
        val parseResult = UniversalConfigParser.parse(configUrl)
        return parseResult.onSuccess { config ->
            val withGroup = config.copy(groupName = targetGroup.ifBlank { "Default" })
            viewModelScope.launch {
                repository.saveConfig(withGroup)
            }
        }
    }

    fun importVlessUrl(url: String, targetGroup: String = "Default"): Result<ServerConfig> {
        return importConfig(url, targetGroup)
    }

    fun importSubscription(
        subUrl: String,
        targetGroup: String,
        onComplete: (Boolean, String, Int) -> Unit
    ) {
        viewModelScope.launch {
            val groupToUse = targetGroup.ifBlank { "Sub-Import" }
            _customGroups.value = _customGroups.value + groupToUse
            val result = SubscriptionFetcher.fetchSubscription(subUrl, groupToUse)
            result.onSuccess { configs ->
                repository.saveAll(configs)
                _selectedGroup.value = groupToUse
                onComplete(true, "Imported ${configs.size} configs successfully.", configs.size)
                // Auto test ping for newly imported configs
                testGroupPing(groupToUse)
            }.onFailure { error ->
                onComplete(false, error.localizedMessage ?: "Failed to fetch subscription.", 0)
            }
        }
    }

    fun saveConfig(config: ServerConfig) {
        viewModelScope.launch {
            repository.saveConfig(config)
        }
    }

    fun deleteConfig(config: ServerConfig) {
        viewModelScope.launch {
            repository.deleteConfig(config)
        }
    }

    fun deleteAllConfigs() {
        viewModelScope.launch {
            repository.deleteAllConfigs()
            _customGroups.value = emptySet()
            _selectedGroup.value = "All"
            AppLogger.i("MainViewModel", "All configs deleted by user.")
        }
    }

    fun setActiveConfig(config: ServerConfig) {
        viewModelScope.launch {
            repository.setActiveConfig(config.id)
        }
    }

    fun testSingleConfigPing(config: ServerConfig) {
        viewModelScope.launch {
            val ping = PingTester.testServerTcpPing(config.address, config.port)
            repository.updatePing(config.id, ping)
        }
    }

    fun testGroupPing(groupName: String?) {
        viewModelScope.launch {
            val currentList = allConfigs.value
            val targetConfigs = if (groupName == null || groupName == "All") {
                currentList
            } else {
                currentList.filter { it.groupName.equals(groupName, ignoreCase = true) }
            }

            if (targetConfigs.isEmpty()) return@launch

            _isGroupPingTesting.value = true
            _groupPingProgress.value = 0f
            var testedCount = 0

            AppLogger.i("MainViewModel", "Starting batch ping test for ${targetConfigs.size} configs (Group: ${groupName ?: "All"})")

            PingTester.batchTestPings(targetConfigs) { config, ping ->
                repository.updatePing(config.id, ping)
                testedCount++
                _groupPingProgress.value = testedCount.toFloat() / targetConfigs.size.toFloat()
            }

            _isGroupPingTesting.value = false
            AppLogger.i("MainViewModel", "Finished batch ping test.")
        }
    }

    fun runPingTest() {
        viewModelScope.launch {
            val targetUrl = AppSettingsManager.getPingUrl()
            _isTestingPing.value = true
            _pingResult.value = "Testing latency via $targetUrl..."
            val result = withContext(Dispatchers.IO) {
                ConnectivityTester.testConnection(targetUrl)
            }
            _isTestingPing.value = false
            if (result.isSuccessful) {
                _pingResult.value = "Ping: ${result.latencyMs}ms | IP: ${result.publicIp ?: "Connected"}"
            } else {
                _pingResult.value = "Failed: ${result.errorMessage ?: "Unreachable"}"
            }
        }
    }

    fun clearLogs() {
        AppLogger.clear()
    }
}
