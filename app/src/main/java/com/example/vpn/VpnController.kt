package com.example.vpn

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.logger.AppLogger
import com.example.model.ConnectionState
import com.example.model.ServerConfig
import com.example.model.VpnStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnController {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _vpnStats = MutableStateFlow(VpnStats())
    val vpnStats: StateFlow<VpnStats> = _vpnStats.asStateFlow()

    private val _selectedServer = MutableStateFlow<ServerConfig?>(null)
    val selectedServer: StateFlow<ServerConfig?> = _selectedServer.asStateFlow()

    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
        AppLogger.i("VpnController", "Connection State changed -> ${state::class.simpleName}")
    }

    fun setSelectedServer(server: ServerConfig?) {
        _selectedServer.value = server
    }

    fun updateStats(txBytes: Long, rxBytes: Long, txPackets: Long, rxPackets: Long, durationSec: Long) {
        _vpnStats.value = VpnStats(
            txBytes = txBytes,
            rxBytes = rxBytes,
            txPackets = txPackets,
            rxPackets = rxPackets,
            durationSeconds = durationSec
        )
    }

    fun resetStats() {
        _vpnStats.value = VpnStats()
    }

    fun startVpn(context: Context, serverConfig: ServerConfig) {
        setSelectedServer(serverConfig)
        val intent = Intent(context, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_CONNECT
            putExtra(V2RayVpnService.EXTRA_SERVER_ID, serverConfig.id)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            AppLogger.i("VpnController", "Sent start VPN intent for ${serverConfig.getDisplayTitle()}")
        } catch (e: Exception) {
            AppLogger.e("VpnController", "Failed to start VPN service: ${e.localizedMessage}", e)
            updateConnectionState(ConnectionState.Error("Failed to start service", e.localizedMessage))
        }
    }

    fun stopVpn(context: Context) {
        val intent = Intent(context, V2RayVpnService::class.java).apply {
            action = V2RayVpnService.ACTION_DISCONNECT
        }
        try {
            context.startService(intent)
            AppLogger.i("VpnController", "Sent stop VPN intent")
        } catch (e: Exception) {
            AppLogger.e("VpnController", "Failed to stop VPN service: ${e.localizedMessage}", e)
        }
    }
}
